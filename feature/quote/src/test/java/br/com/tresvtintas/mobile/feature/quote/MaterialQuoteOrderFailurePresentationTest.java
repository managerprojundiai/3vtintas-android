package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import org.junit.Test;

public final class MaterialQuoteOrderFailurePresentationTest {
    @Test
    public void exposesServerGuidanceAndResetsDeterministicConflict() {
        MaterialQuoteOrderFailurePresentation presentation =
                MaterialQuoteOrderFailurePresentation.from(
                        new OrderException(
                                OrderFailureKind.CONFLICT,
                                "Request rejected.",
                                "a848be6b-b24c-4d3a-8448-9d8ae5147d25",
                                "Vincule um vendedor ativo.",
                                null));

        assertEquals(
                "Server-authored safe guidance must remain visible.",
                "Vincule um vendedor ativo.",
                presentation.serverDetail().orElseThrow());
        assertTrue(
                "A corrected business precondition needs a fresh key.",
                presentation.resetIdempotency());
        assertTrue(
                "The request correlation code must remain available.",
                presentation.requestId().isPresent());
    }

    @Test
    public void preservesIdempotencyAcrossAmbiguousNetworkFailure() {
        MaterialQuoteOrderFailurePresentation presentation =
                MaterialQuoteOrderFailurePresentation.from(
                        new OrderException(
                                OrderFailureKind.NETWORK,
                                "Network interrupted."));

        assertFalse(
                "An ambiguous transport failure must replay the same command.",
                presentation.resetIdempotency());
        assertEquals(
                "Network failures use a stable local fallback.",
                R.string.quote_order_failure_network,
                presentation.fallbackMessage());
    }
}
