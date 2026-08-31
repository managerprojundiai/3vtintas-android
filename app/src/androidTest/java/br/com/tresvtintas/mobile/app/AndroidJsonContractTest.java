package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.dto.AccountDevicePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeRequest;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AndroidJsonContractTest {
    private static final String UUID = "123e4567-e89b-42d3-a456-426614174000";
    private static final String NONCE =
            "3vn1_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO12";

    @Test
    public void desugaredRecordsRoundTripOnAndroidRuntime() throws Exception {
        ObjectMapper mapper = MobileApiFactory.objectMapper();

        String request = mapper.writeValueAsString(
                new AuthChallengeRequest(UUID));
        AuthChallengeResponse response = mapper.readValue(
                """
                {
                  "challengeId": "123e4567-e89b-42d3-a456-426614174001",
                  "nonce": "%s",
                  "expiresAt": "2026-07-29T18:00:00Z",
                  "googleServerClientId": "pilot.apps.googleusercontent.com"
                }
                """.formatted(NONCE),
                AuthChallengeResponse.class);

        assertEquals("{\"installationId\":\"" + UUID + "\"}", request);
        assertEquals(
                "123e4567-e89b-42d3-a456-426614174001",
                response.challengeId());
        assertEquals(NONCE, response.nonce());
        assertEquals(
                "pilot.apps.googleusercontent.com",
                response.googleServerClientId());
    }

    @Test
    public void accountSecurityPagesDeserializeOnAndroidRuntime()
            throws Exception {
        ObjectMapper mapper = MobileApiFactory.objectMapper();

        AccountDevicePageDto devices = mapper.readValue(
                """
                {
                  "items": [{
                    "id": "__DEVICE_ID__",
                    "displayName": "Xiaomi 24040RN64Y",
                    "manufacturer": "Xiaomi",
                    "model": "24040RN64Y",
                    "androidApi": 35,
                    "appVersion": "0.39.1-f9-pilot-staging",
                    "status": "active",
                    "revision": 1,
                    "registeredAt": "2026-07-29T19:42:19.000Z",
                    "lastSeenAt": "2026-08-02T02:50:11.000Z",
                    "revokedAt": null,
                    "current": true
                  }],
                  "nextCursor": null
                }
                """.replace("__DEVICE_ID__", UUID),
                AccountDevicePageDto.class);
        AccountSessionPageDto sessions = mapper.readValue(
                """
                {
                  "items": [{
                    "id": "123e4567-e89b-42d3-a456-426614174001",
                    "deviceId": "__DEVICE_ID__",
                    "authMethod": "google",
                    "status": "active",
                    "revision": 1,
                    "issuedAt": "2026-07-29T19:42:19.000Z",
                    "lastSeenAt": "2026-08-02T02:50:11.000Z",
                    "idleExpiresAt": "2026-09-01T02:50:11.000Z",
                    "absoluteExpiresAt": "2027-01-25T19:42:19.000Z",
                    "endedAt": null,
                    "current": true
                  }],
                  "nextCursor": null
                }
                """.replace("__DEVICE_ID__", UUID),
                AccountSessionPageDto.class);

        assertEquals("device count", 1, devices.items().size());
        assertEquals("session count", 1, sessions.items().size());
        assertEquals("device id", UUID, devices.items().get(0).id());
        assertEquals(
                "session device id",
                UUID,
                sessions.items().get(0).deviceId());
    }
}
