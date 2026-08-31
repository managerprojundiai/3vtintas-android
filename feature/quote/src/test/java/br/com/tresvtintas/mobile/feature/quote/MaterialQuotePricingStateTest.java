package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.quote.MaterialQuotePriceListOption;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingContext;
import java.util.List;
import org.junit.Test;

public final class MaterialQuotePricingStateTest {
    private static final String PRIMARY = "11111111-1111-1111-1111-111111111111";
    private static final String SECONDARY = "22222222-2222-2222-2222-222222222222";

    @Test
    public void legacyFlowNeedsNoSelection() {
        MaterialQuotePricingState state = MaterialQuotePricingState.legacy();
        assertFalse(state.visible());
        assertTrue(state.ready());
        assertTrue(state.claim().isEmpty());
    }

    @Test
    public void primaryIsAutomaticWhenPolicyDoesNotRequireChoice() {
        MaterialQuotePricingState state = MaterialQuotePricingState.from(context(false));
        assertTrue(state.ready());
        assertEquals("Tabela 2", state.selectedOption().orElseThrow().name());
        assertTrue(state.claim().orElseThrow()
                .selectedPriceListVersionPublicId().isEmpty());
    }

    @Test
    public void salespersonMustExplicitlyChooseOneActiveTable() {
        MaterialQuotePricingState state = MaterialQuotePricingState.from(context(true));
        assertFalse(state.ready());
        MaterialQuotePricingState selected = state.select(SECONDARY);
        assertTrue(selected.ready());
        assertEquals(SECONDARY, selected.claim().orElseThrow()
                .selectedPriceListVersionPublicId().orElseThrow());
    }

    @Test
    public void restoresOnlyVersionsPresentInTheCurrentPolicy() {
        MaterialQuotePricingState state = MaterialQuotePricingState.from(context(true));

        assertTrue("An active frozen table can be restored.",
                state.supports(PRIMARY));
        assertFalse("A retired table must not crash the editor or be resubmitted.",
                state.supports("33333333-3333-3333-3333-333333333333"));
    }

    private static MaterialQuotePricingContext context(boolean required) {
        return new MaterialQuotePricingContext(
                3L,
                true,
                "ACTIVE",
                7,
                required,
                List.of(
                        new MaterialQuotePriceListOption(
                                PRIMARY, "2", "Tabela 2", 4, true),
                        new MaterialQuotePriceListOption(
                                SECONDARY, "3", "Tabela 3", 2, false)));
    }
}
