package br.com.tresvtintas.mobile.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;

public final class MobileApiFactoryTest {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN = "a".repeat(80);
    private static final String REFRESHED_TOKEN = "b".repeat(80);
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void sendsVersionAndRequestMetadataWithoutLeakingBearerToPublicRoute()
            throws Exception {
        server.enqueue(jsonResponse(metaJson()));
        MobileApi api = api(Optional.of(TOKEN));

        Response<?> response = api.meta().execute();
        RecordedRequest request = server.takeRequest();

        assertTrue("Metadata response must succeed.", response.isSuccessful());
        assertPublicMetadataRequest(request);
    }

    @Test
    public void addsBearerOnlyToAnnotatedEndpoint() throws Exception {
        server.enqueue(jsonResponse(meJson()));
        MobileApi api = api(Optional.of(TOKEN));

        Response<?> response = api.me().execute();
        RecordedRequest request = server.takeRequest();

        assertTrue("Authenticated response must succeed.", response.isSuccessful());
        assertAuthenticatedMeRequest(request);
    }

    @Test
    public void blocksAuthenticatedRequestBeforeNetworkWhenTokenIsMissing()
            throws Exception {
        MobileApi api = api(Optional.empty());

        assertThrows(
                "Missing token must block the request.",
                AuthenticationRequiredException.class,
                () -> api.me().execute());
        assertNoNetworkRequest();
    }

    @Test
    public void rejectsUnknownResponseFields() throws Exception {
        server.enqueue(jsonResponse(metaJson().replace(
                "\"maintenance\":false", "\"maintenance\":false,\"unexpected\":true")));
        MobileApi api = api(Optional.empty());

        assertThrows(
                "Unknown response properties must fail strict parsing.",
                IOException.class,
                () -> api.meta().execute());
        assertOneNetworkRequest();
    }

    @Test
    public void refreshesOneAuthenticatedRequestAfterUnauthorizedResponse()
            throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(jsonResponse(meJson()));
        AtomicReference<Optional<String>> token = new AtomicReference<>(Optional.of(TOKEN));
        AtomicInteger refreshes = new AtomicInteger();
        MobileApi api = api(token::get, rejected -> {
            assertEquals("Authenticator must identify the rejected token.", TOKEN, rejected);
            refreshes.incrementAndGet();
            token.set(Optional.of(REFRESHED_TOKEN));
            return token.get();
        });

        Response<?> response = api.me().execute();
        RecordedRequest rejected = server.takeRequest();
        RecordedRequest retried = server.takeRequest();

        assertTrue("Retried authenticated response must succeed.", response.isSuccessful());
        assertEquals("Exactly one refresh must occur.", 1, refreshes.get());
        assertEquals("First request uses the original token.", BEARER_PREFIX + TOKEN,
                rejected.getHeader(AUTHORIZATION_HEADER));
        assertEquals("Retry uses the rotated token.", BEARER_PREFIX + REFRESHED_TOKEN,
                retried.getHeader(AUTHORIZATION_HEADER));
    }

    @Test
    public void neverRefreshesPublicUnauthorizedResponse() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        AtomicInteger refreshes = new AtomicInteger();
        MobileApi api = api(
                () -> Optional.of(TOKEN),
                rejected -> {
                    refreshes.incrementAndGet();
                    return Optional.of(REFRESHED_TOKEN);
                });

        Response<?> response = api.meta().execute();

        assertEquals("Public 401 must remain visible to the caller.", 401, response.code());
        assertEquals("Public routes must never invoke bearer refresh.", 0, refreshes.get());
        assertOneNetworkRequest();
    }

    @Test
    public void proactivelyRefreshesBeforeProtectedRequestWhenMemoryTokenIsUnavailable()
            throws Exception {
        server.enqueue(jsonResponse(meJson()));
        AtomicReference<Optional<String>> token = new AtomicReference<>(Optional.empty());
        AtomicInteger refreshes = new AtomicInteger();
        MobileApi api = api(token::get, rejected -> {
            assertNull("Proactive refresh has no rejected bearer.", rejected);
            refreshes.incrementAndGet();
            token.set(Optional.of(REFRESHED_TOKEN));
            return token.get();
        });

        Response<?> response = api.me().execute();
        RecordedRequest request = server.takeRequest();

        assertTrue("Protected request must continue after proactive refresh.",
                response.isSuccessful());
        assertEquals("Exactly one proactive refresh must occur.", 1, refreshes.get());
        assertEquals("Request must use the rotated bearer.", BEARER_PREFIX + REFRESHED_TOKEN,
                request.getHeader(AUTHORIZATION_HEADER));
    }

    @Test
    public void authenticatesEventStreamsAndPreservesStreamingHeaders()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("id: 42\n\n"));
        MobileNetworkClient client = client(
                () -> Optional.of(TOKEN),
                null);

        try (okhttp3.Response response = client.newAuthenticatedEventStreamCall(
                "attendance/events",
                Optional.of("41")).execute()) {
            assertTrue("Event stream must open.", response.isSuccessful());
        }
        RecordedRequest request = server.takeRequest();
        assertEquals(
                "Event stream must receive the bearer.",
                BEARER_PREFIX + TOKEN,
                request.getHeader(AUTHORIZATION_HEADER));
        assertEquals(
                "Event stream media type must not be overwritten.",
                "text/event-stream",
                request.getHeader("Accept"));
        assertEquals(
                "Resume cursor must use the standard header.",
                "41",
                request.getHeader("Last-Event-ID"));
        assertEquals(
                "Stream must remain below the versioned API path.",
                "/api/mobile/v1/attendance/events",
                request.getPath());
    }

    @Test
    public void blocksUnsafeOrUnauthenticatedEventStreamsBeforeNetwork()
            throws Exception {
        MobileNetworkClient missing = client(
                Optional::<String>empty,
                null);
        assertThrows(
                AuthenticationRequiredException.class,
                () -> missing.newAuthenticatedEventStreamCall(
                        "attendance/events",
                        Optional.empty()).execute());
        assertThrows(
                IllegalArgumentException.class,
                () -> missing.newAuthenticatedEventStreamCall(
                        "../auth/refresh",
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> missing.newAuthenticatedEventStreamCall(
                        "attendance/events",
                        Optional.of("cursor")));
        assertNoNetworkRequest();
    }

    private MobileApi api(Optional<String> token) {
        return api(() -> token, null);
    }

    private MobileApi api(
            BearerTokenProvider tokenProvider,
            BearerTokenRefresher tokenRefresher) {
        return client(tokenProvider, tokenRefresher).api();
    }

    private MobileNetworkClient client(
            BearerTokenProvider tokenProvider,
            BearerTokenRefresher tokenRefresher) {
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.2.0-auth-foundation",
                3,
                true);
        return MobileApiFactory.createClient(
                configuration,
                tokenProvider,
                tokenRefresher);
    }

    private static void assertPublicMetadataRequest(RecordedRequest request) {
        assertEquals(
                "Semantic app version header must be present.",
                "0.2.0-auth-foundation",
                request.getHeader("X-3V-App-Version"));
        assertEquals(
                "Monotonic app version code header must be present.",
                "3",
                request.getHeader("X-3V-App-Version-Code"));
        assertNotNull(
                "Request ID must be a UUID.",
                UUID.fromString(request.getHeader("X-Request-Id")));
        assertEquals(
                "Client must explicitly request JSON.",
                "application/json",
                request.getHeader("Accept"));
        assertFalse(
                "Public route must not receive a bearer header.",
                request.getHeaders().names().contains(AUTHORIZATION_HEADER));
    }

    private static void assertAuthenticatedMeRequest(RecordedRequest request) {
        assertEquals(
                "Annotated route must receive the bearer.",
                BEARER_PREFIX + TOKEN,
                request.getHeader(AUTHORIZATION_HEADER));
        assertEquals(
                "Retrofit must preserve the v1 base path.",
                "/api/mobile/v1/me",
                request.getPath());
    }

    private void assertNoNetworkRequest() {
        assertEquals(
                "Blocked authenticated request must not reach the server.",
                0,
                server.getRequestCount());
    }

    private void assertOneNetworkRequest() {
        assertEquals(
                "Strict parsing failure happens after one server response.",
                1,
                server.getRequestCount());
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String metaJson() {
        return "{"
                + "\"apiVersion\":\"v1\","
                + "\"contractVersion\":\"0.5.0\","
                + "\"minimumSupportedAppVersion\":\"0.1.0\","
                + "\"minimumSupportedAppVersionCode\":2,"
                + "\"minimumSupportedAndroidApiLevel\":26,"
                + "\"maintenance\":false,"
                + "\"serverTime\":\"2026-07-25T12:00:00Z\""
                + "}";
    }

    private static String meJson() {
        return "{"
                + "\"user\":{\"id\":1,\"name\":\"3V\",\"email\":null,\"role\":\"admin\"},"
                + "\"session\":{\"id\":\"550e8400-e29b-41d4-a716-446655440000\","
                + "\"deviceId\":\"550e8400-e29b-41d4-a716-446655440001\"}"
                + "}";
    }
}
