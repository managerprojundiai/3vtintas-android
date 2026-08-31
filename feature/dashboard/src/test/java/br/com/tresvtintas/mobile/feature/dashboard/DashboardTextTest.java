package br.com.tresvtintas.mobile.feature.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.dashboard.DashboardFailureKind;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.dashboard.DashboardVisibility;
import java.math.BigDecimal;
import org.junit.Test;

public final class DashboardTextTest {
    @Test
    public void formatsBrazilianMoneyWithoutLosingDecimalPrecision() {
        assertEquals(
                "Dashboard money must use the Brazilian currency format.",
                "R$\u00a01.234,56",
                DashboardText.money(new BigDecimal("1234.56")));
    }

    @Test
    public void mapsEveryServerDerivedScopeToExplicitCopy() {
        assertEquals(
                "Team visibility must have an explicit label.",
                R.string.dashboard_visibility_team,
                DashboardText.visibility(DashboardVisibility.TEAM));
        assertEquals(
                "Corporate finance must have an explicit label.",
                R.string.dashboard_scope_corporate,
                DashboardText.financeScope(
                        DashboardSnapshot.FinanceScope.CORPORATE));
        assertEquals(
                "Global agenda must have an explicit label.",
                R.string.dashboard_scope_all,
                DashboardText.appointmentScope(
                        DashboardSnapshot.AppointmentScope.ALL));
    }

    @Test
    public void mapsSecurityFailuresWithoutLeakingTechnicalMessages() {
        assertEquals(
                "Revoked access must direct the user back to a valid session.",
                R.string.dashboard_error_session,
                DashboardText.failure(
                        DashboardFailureKind.ACCESS_REVOKED));
        assertEquals(
                "Protocol failures must use a generic safe message.",
                R.string.dashboard_error_generic,
                DashboardText.failure(DashboardFailureKind.PROTOCOL));
    }

    @Test
    public void runtimeRequiresRepositoryAndWorker() {
        assertThrows(
                "A runtime without a repository must be rejected.",
                NullPointerException.class,
                () -> new DashboardFeatureRuntime(null, Runnable::run));
        assertThrows(
                "A runtime without a worker must be rejected.",
                NullPointerException.class,
                () -> new DashboardFeatureRuntime(() -> null, null));
    }
}
