package br.com.tresvtintas.mobile.core.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RetrofitBootstrapRemoteTest {
    private MockWebServer server;
    private RetrofitBootstrapRemote remote;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        MobileApi api = MobileApiFactory.create(
                new NetworkConfiguration(
                        server.url("/api/mobile/v1/").toString(),
                        "0.5.0",
                        5,
                        true),
                () -> Optional.of("a".repeat(80)));
        remote = new RetrofitBootstrapRemote(api);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void loadsStrictBootstrapContractWithBearer() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(successBody()));

        assertEquals("Bootstrap user must be parsed.", 41, remote.load().user().id());
        assertEquals(
                "Protected bootstrap must use the bearer token.",
                "Bearer " + "a".repeat(80),
                server.takeRequest().getHeader("Authorization"));
    }

    @Test
    public void mapsProblemDetailsWithoutLeakingBody() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {
                          "type":"https://3vtintas.com.br/problems/auth-required",
                          "title":"Autenticação necessária",
                          "status":401,
                          "detail":"Entre novamente.",
                          "instance":"urn:3v:request:00000000-0000-4000-8000-000000000051",
                          "code":"AUTH_REQUIRED",
                          "requestId":"00000000-0000-4000-8000-000000000051"
                        }
                        """));

        BootstrapException failure = assertThrows(
                "Problem Details authentication failure must be mapped.",
                BootstrapException.class,
                remote::load);

        assertEquals(
                "Authentication failure kind must be preserved.",
                BootstrapFailureKind.AUTH_REJECTED,
                failure.kind());
        assertEquals(
                "Request correlation ID must be preserved.",
                "00000000-0000-4000-8000-000000000051",
                failure.requestId().orElseThrow());
    }

    private static String successBody() {
        return """
                {
                  "user":{
                    "id":41,
                    "name":"Pessoa",
                    "email":"pessoa@example.test",
                    "role":"salesperson"
                  },
                  "session":{
                    "id":"00000000-0000-4000-8000-000000000041",
                    "deviceId":"00000000-0000-4000-8000-000000000042"
                  },
                  "authorization":{
                    "capabilities":["catalog.read","quote.create"],
                    "organizationAccess":{
                      "mode":"assigned",
                      "organizations":[{
                        "id":9,
                        "name":"3V Centro",
                        "slug":"3v-centro",
                        "membershipRole":"salesperson"
                      }],
                      "hasMore":false,
                      "defaultOrganizationId":9
                    },
                    "revision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                  },
                  "api":{
                    "apiVersion":"v1",
                    "contractVersion":"0.5.0",
                    "minimumSupportedAppVersion":"0.1.0",
                    "minimumSupportedAppVersionCode":2,
                    "minimumSupportedAndroidApiLevel":26,
                    "maintenance":false,
                    "serverTime":"2026-07-25T13:00:00.000Z"
                  }
                }
                """;
    }
}
