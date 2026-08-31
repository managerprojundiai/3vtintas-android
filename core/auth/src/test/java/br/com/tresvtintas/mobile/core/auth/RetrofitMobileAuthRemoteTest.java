package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RetrofitMobileAuthRemoteTest {
    private static final String REQUEST_ID = "550e8400-e29b-41d4-a716-446655440004";
    private MockWebServer server;
    private RetrofitMobileAuthRemote remote;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.3.0-google-auth",
                4,
                true);
        remote = new RetrofitMobileAuthRemote(MobileApiFactory.create(
                configuration,
                () -> Optional.of(AuthTestFixtures.ACCESS_TOKEN_A)));
    }

    @After
    public void tearDown() throws IOException {
        server.close();
    }

    @Test
    public void mapsBoundedProblemDetailsToStableFailureAndRequestId() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader("Content-Type", "application/problem+json")
                .setBody(problemDetails()));

        AuthException failure = assertThrows(
                AuthException.class,
                () -> remote.createChallenge(AuthTestFixtures.INSTALLATION_ID));

        assertEquals(AuthFailureKind.RATE_LIMITED, failure.kind());
        assertEquals(REQUEST_ID, failure.requestId().orElseThrow());
    }

    @Test
    public void rejectsUnstructuredServerErrorsAsProtocolFailure() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader("Content-Type", "text/plain")
                .setBody("untrusted"));

        AuthException failure = assertThrows(
                AuthException.class,
                () -> remote.createChallenge(AuthTestFixtures.INSTALLATION_ID));

        assertEquals(AuthFailureKind.PROTOCOL, failure.kind());
    }

    private static String problemDetails() {
        return "{"
                + "\"type\":\"https://www.3vtintas.com.br/problems/rate-limited\","
                + "\"title\":\"Muitas tentativas\","
                + "\"status\":429,"
                + "\"detail\":\"Aguarde antes de tentar novamente.\","
                + "\"instance\":\"urn:3v:request:" + REQUEST_ID + "\","
                + "\"code\":\"RATE_LIMITED\","
                + "\"requestId\":\"" + REQUEST_ID + "\""
                + "}";
    }
}
