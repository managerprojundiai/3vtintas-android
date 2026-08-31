package br.com.tresvtintas.mobile.core.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public final class MaterialQuoteDraftTest {
    private static final String CUSTOMER = "Cliente";

    @Test
    public void keepsOnlyProductQuantityAndDisplayEstimateInDraft() {
        MaterialQuoteDraftLine line = new MaterialQuoteDraftLine(
                7,
                "Tinta Premium",
                new BigDecimal("2.50"),
                new BigDecimal("19.90"));
        MaterialQuoteDraft draft = new MaterialQuoteDraft(
                8,
                CUSTOMER,
                Optional.of("  Reforma  "),
                Optional.empty(),
                Optional.empty(),
                List.of(line));

        assertEquals(
                "Title must be normalized.",
                "Reforma",
                draft.title().orElseThrow());
        assertEquals(
                "Estimate must use exact decimal multiplication.",
                new BigDecimal("49.75"),
                line.estimate());
    }

    @Test
    public void rejectsRepeatedProducts() {
        MaterialQuoteDraftLine line = new MaterialQuoteDraftLine(
                7,
                "Tinta",
                BigDecimal.ONE,
                BigDecimal.TEN.setScale(2));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialQuoteDraft(
                        8,
                        CUSTOMER,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(line, line)));
    }

    @Test
    public void allowsSameProductInDifferentTintContexts() {
        MaterialQuoteDraft draft = new MaterialQuoteDraft(
                8,
                CUSTOMER,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(
                        tintLine(7, 101, 201, "Cinza Crômio"),
                        tintLine(7, 102, 201, "Branco"),
                        tintLine(7, 101, 202, "Cinza Crômio")));

        assertEquals(
                "Each color and package context is a separate sales line.",
                3,
                draft.items().size());
    }

    @Test
    public void rejectsRepeatedTintIdentity() {
        MaterialQuoteDraftLine line = tintLine(
                7,
                101,
                201,
                "Cinza Crômio");

        assertThrows(
                "The same product, color and context cannot repeat.",
                IllegalArgumentException.class,
                () -> new MaterialQuoteDraft(
                        8,
                        CUSTOMER,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(line, line)));
    }

    @Test
    public void preservesServerPricingSelectionWithoutAcceptingMoney() {
        String version = UUID.randomUUID().toString();
        MaterialQuotePricingSelection pricing =
                new MaterialQuotePricingSelection(7, Optional.of(version));
        MaterialQuoteDraft draft = new MaterialQuoteDraft(
                8,
                CUSTOMER,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(pricing),
                List.of(new MaterialQuoteDraftLine(
                        7,
                        "Tinta",
                        BigDecimal.ONE,
                        BigDecimal.TEN.setScale(2))));

        assertEquals(
                "Policy revision must remain inseparable from selection.",
                7,
                draft.pricing().orElseThrow().expectedPolicyRevision());
        assertEquals(
                "Only the opaque table-version identity is retained.",
                version,
                draft.pricing().orElseThrow()
                        .selectedPriceListVersionPublicId().orElseThrow());
    }

    private static MaterialQuoteDraftLine tintLine(
            long productId,
            long colorId,
            long contextId,
            String colorName) {
        return new MaterialQuoteDraftLine(
                productId,
                "LKC Emborrachada",
                BigDecimal.ONE,
                new BigDecimal("535.90"),
                Optional.of(new MaterialQuoteTintSelection(
                        colorId,
                        colorName,
                        contextId,
                        "Emborrachada",
                        "Fosco",
                        "18 L")));
    }
}
