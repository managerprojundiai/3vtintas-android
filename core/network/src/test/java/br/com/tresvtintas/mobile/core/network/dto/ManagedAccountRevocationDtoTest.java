package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class ManagedAccountRevocationDtoTest {
    private static final String ACTION_ID =
            "30000000-0000-4000-8000-000000000001";
    private static final String RESOURCE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String CHALLENGE_ID =
            "40000000-0000-4000-8000-000000000001";
    private static final String EXPIRY = "2026-07-31T13:00:00Z";
    private static final String TOKEN = "3vsu1_" + "t".repeat(43);

    @Test
    public void retainsServerBoundPreviewAndRevision() {
        ManagedAccountRevocationDtos.PreparedAction preview =
                new ManagedAccountRevocationDtos.PreparedAction(
                        ACTION_ID,
                        "device",
                        new ManagedAccountRevocationDtos.Target(
                                42,
                                "Usuário alvo"),
                        new ManagedAccountRevocationDtos.Resource(
                                RESOURCE_ID,
                                "Aparelho corporativo",
                                7),
                        "Todas as sessões serão encerradas.",
                        EXPIRY);

        assertEquals(
                "The preview must retain the server security revision.",
                7,
                preview.resource().revision());
        assertThrows(
                "Unknown resource kinds must fail closed.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationDtos.PreparedAction(
                        ACTION_ID,
                        "organization",
                        preview.target(),
                        preview.resource(),
                        preview.consequence(),
                        EXPIRY));
    }

    @Test
    public void validatesChallengeGrantAndExplicitConfirmations() {
        ManagedAccountRevocationDtos.ChallengeResponse challenge =
                new ManagedAccountRevocationDtos.ChallengeResponse(
                        CHALLENGE_ID,
                        "3vn1_" + "n".repeat(43),
                        "473842962788-example.apps.googleusercontent.com",
                        EXPIRY);
        ManagedAccountRevocationDtos.ExecuteRequest request =
                ManagedAccountRevocationDtos.ExecuteRequest.confirmed(
                        TOKEN);

        assertEquals(
                "The nonce must remain bound to the challenge response.",
                CHALLENGE_ID,
                challenge.challengeId());
        assertEquals(
                "Execution must carry only the ephemeral grant.",
                TOKEN,
                request.stepUpToken());
        assertThrows(
                "A weak credential must fail before transport.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationDtos.VerifyRequest(
                        CHALLENGE_ID,
                        "weak"));
        assertThrows(
                "The execution confirmation is not caller-defined.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationDtos.ExecuteRequest(
                        "CONFIRM_AGENT_ACTION",
                        TOKEN));
    }

    @Test
    public void rejectsNonMutatingOrUnversionedResults() {
        assertThrows(
                "The server result must prove a real mutation.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationDtos.Result(
                        ACTION_ID,
                        "session",
                        42,
                        RESOURCE_ID,
                        false,
                        8));
        assertThrows(
                "A result without a security revision is invalid.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationDtos.Result(
                        ACTION_ID,
                        "session",
                        42,
                        RESOURCE_ID,
                        true,
                        0));
    }
}
