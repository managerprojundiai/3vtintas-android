package br.com.tresvtintas.mobile.feature.finance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.finance.FinanceExperience;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class FinanceRouteTest {
    @Test
    public void onlyWritableCorporateScopeMayCreate() {
        assertFalse(FinanceRoute.corporateGlobal().canCreate());
        assertTrue(FinanceRoute.corporate(7, "Loja Centro").canCreate());
        assertTrue(FinanceRoute.personal().canCreate());
    }

    @Test
    public void personalRouteCannotCarryCorporateOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinanceRoute(
                        FinanceExperience.PERSONAL,
                        OptionalLong.of(7),
                        Optional.of("Loja Centro")));
    }
}
