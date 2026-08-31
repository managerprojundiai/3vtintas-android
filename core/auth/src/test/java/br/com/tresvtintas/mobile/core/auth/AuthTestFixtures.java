package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.CredentialSet;
import br.com.tresvtintas.mobile.core.network.dto.DeviceInfo;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.time.Instant;

final class AuthTestFixtures {
    static final String INSTALLATION_ID = "550e8400-e29b-41d4-a716-446655440000";
    static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440001";
    static final String DEVICE_ID = "550e8400-e29b-41d4-a716-446655440002";
    static final String ACCESS_TOKEN_A = "a".repeat(80);
    static final String ACCESS_TOKEN_B = "b".repeat(80);
    static final String REFRESH_TOKEN_A = "3vr1_" + "a".repeat(43);
    static final String REFRESH_TOKEN_B = "3vr1_" + "b".repeat(43);
    static final Instant NOW = Instant.parse("2026-07-25T12:00:00Z");

    private AuthTestFixtures() {
        throw new AssertionError("No instances.");
    }

    static CredentialSet credentials(String accessToken, String refreshToken) {
        return new CredentialSet(
                accessToken,
                NOW.plusSeconds(300).toString(),
                refreshToken,
                NOW.plusSeconds(86_400).toString(),
                NOW.plusSeconds(172_800).toString(),
                SESSION_ID,
                DEVICE_ID);
    }

    static AuthChallengeResponse challenge() {
        return new AuthChallengeResponse(
                "550e8400-e29b-41d4-a716-446655440003",
                "3vn1_" + "n".repeat(43),
                NOW.plusSeconds(300).toString(),
                "client.apps.googleusercontent.com");
    }

    static AuthenticatedUser user() {
        return new AuthenticatedUser(7, "Vendedor 3V", "vendedor@3vtintas.com.br", "salesperson");
    }

    static MeResponse me() {
        return new MeResponse(user(), new SessionIdentity(SESSION_ID, DEVICE_ID));
    }

    static DeviceInfo device() {
        return new DeviceInfo(
                INSTALLATION_ID,
                "Android de teste",
                "3V",
                "Reference",
                35,
                "0.3.0-google-auth");
    }
}
