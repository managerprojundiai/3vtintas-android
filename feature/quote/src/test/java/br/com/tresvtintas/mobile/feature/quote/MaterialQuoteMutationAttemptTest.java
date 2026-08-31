package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraft;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraftLine;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSelection;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintSelection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public final class MaterialQuoteMutationAttemptTest {
    private static final String PREVIEW = "a".repeat(64);

    @Test
    public void reusesKeyOnlyForSameDraftAndRevision() {
        MaterialQuoteDraft draft = new MaterialQuoteDraft(
                7,
                "Cliente",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(new MaterialQuoteDraftLine(
                        9,
                        "Tinta",
                        BigDecimal.ONE,
                        new BigDecimal("20.00"))));
        MaterialQuoteMutationAttempt attempt = new MaterialQuoteMutationAttempt();
        String first = attempt.keyFor(draft, 1, PREVIEW);
        assertEquals(first, attempt.keyFor(draft, 1, PREVIEW));
        assertNotEquals(first, attempt.keyFor(draft, 2, PREVIEW));
        assertNotEquals(
                first,
                attempt.keyFor(draft, 1, "b".repeat(64)));
    }

    @Test
    public void changesKeyForTableOrTintIdentity() {
        String firstVersion = UUID.randomUUID().toString();
        MaterialQuoteMutationAttempt attempt = new MaterialQuoteMutationAttempt();
        MaterialQuoteDraft first = draft(firstVersion, 101, 201);
        String initial = attempt.keyFor(first, 1, PREVIEW);

        assertNotEquals(
                "Changing the frozen table must create another operation.",
                initial,
                attempt.keyFor(
                        draft(UUID.randomUUID().toString(), 101, 201),
                        1,
                        PREVIEW));
        assertNotEquals(
                "Changing color or package context must create another operation.",
                attempt.keyFor(first, 1, PREVIEW),
                attempt.keyFor(draft(firstVersion, 102, 202), 1, PREVIEW));
    }

    private static MaterialQuoteDraft draft(
            String version,
            long colorId,
            long contextId) {
        return new MaterialQuoteDraft(
                7,
                "Cliente",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new MaterialQuotePricingSelection(
                        11,
                        Optional.of(version))),
                List.of(new MaterialQuoteDraftLine(
                        9,
                        "LKC Emborrachada",
                        BigDecimal.ONE,
                        new BigDecimal("535.90"),
                        Optional.of(new MaterialQuoteTintSelection(
                                colorId,
                                "Cinza Crômio",
                                contextId,
                                "Emborrachada",
                                "Fosco",
                                "18 L")))));
    }
}
