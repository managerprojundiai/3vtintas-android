package br.com.tresvtintas.mobile.core.accountaccess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import org.junit.Test;

public final class ManagedAccountRevocationModelTest {
    private static final String ACTION_ID =
            "30000000-0000-4000-8000-000000000001";
    private static final String RESOURCE_ID =
            "10000000-0000-4000-8000-000000000001";

    @Test
    public void previewBindsTargetResourceRevisionAndExpiry() {
        ManagedAccountRevocationPreview preview = preview();

        assertFalse(
                "The preview is usable before its server expiry.",
                preview.isExpired(
                        Instant.parse("2026-07-31T12:59:59Z")));
        assertTrue(
                "The preview expires exactly at its server boundary.",
                preview.isExpired(
                        Instant.parse("2026-07-31T13:00:00Z")));
        assertThrows(
                "A missing security revision must fail closed.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationPreview(
                        ACTION_ID,
                        AccountAccessView.DEVICES,
                        42,
                        "Usuário alvo",
                        RESOURCE_ID,
                        "Aparelho",
                        0,
                        "Revoga sessões.",
                        Instant.parse("2026-07-31T13:00:00Z")));
    }

    @Test
    public void resultRequiresChangedStateAndNewRevision() {
        ManagedAccountRevocationResult result =
                new ManagedAccountRevocationResult(
                        ACTION_ID,
                        AccountAccessView.DEVICES,
                        42,
                        RESOURCE_ID,
                        true,
                        8);

        assertTrue(
                "A confirmed result must represent a mutation.",
                result.changed());
        assertThrows(
                "An unchanged administrative result is invalid.",
                IllegalArgumentException.class,
                () -> new ManagedAccountRevocationResult(
                        ACTION_ID,
                        AccountAccessView.DEVICES,
                        42,
                        RESOURCE_ID,
                        false,
                        8));
    }

    private static ManagedAccountRevocationPreview preview() {
        return new ManagedAccountRevocationPreview(
                ACTION_ID,
                AccountAccessView.DEVICES,
                42,
                "Usuário alvo",
                RESOURCE_ID,
                "Aparelho corporativo",
                7,
                "Todas as sessões serão encerradas.",
                Instant.parse("2026-07-31T13:00:00Z"));
    }
}
