package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public final class AccountSecurityDtoJsonTest {
    private static final String DEVICE_ID =
            "123e4567-e89b-42d3-a456-426614174000";
    private static final String SESSION_ID =
            "123e4567-e89b-42d3-a456-426614174001";

    @Test
    public void readsCurrentDevicePageFromPublicContract() throws Exception {
        ObjectMapper mapper = MobileApiFactory.objectMapper();

        AccountDevicePageDto page = mapper.readValue(
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
                """.replace("__DEVICE_ID__", DEVICE_ID),
                AccountDevicePageDto.class);

        assertEquals("device count", 1, page.items().size());
        assertEquals("device id", DEVICE_ID, page.items().get(0).id());
        assertTrue("current device", page.items().get(0).current());
    }

    @Test
    public void readsCurrentSessionPageFromPublicContract() throws Exception {
        ObjectMapper mapper = MobileApiFactory.objectMapper();

        AccountSessionPageDto page = mapper.readValue(
                """
                {
                  "items": [{
                    "id": "__SESSION_ID__",
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
                """
                        .replace("__SESSION_ID__", SESSION_ID)
                        .replace("__DEVICE_ID__", DEVICE_ID),
                AccountSessionPageDto.class);

        assertEquals("session count", 1, page.items().size());
        assertEquals("session id", SESSION_ID, page.items().get(0).id());
        assertTrue("current session", page.items().get(0).current());
    }
}
