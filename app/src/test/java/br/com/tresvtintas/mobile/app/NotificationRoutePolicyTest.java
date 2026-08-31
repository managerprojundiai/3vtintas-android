package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.notifications.NotificationRoute;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class NotificationRoutePolicyTest {
    @Test
    public void permitsOnlyDestinationsPresentInTheAuthorizedShell() {
        assertEquals(
                "Attendance must require its server-authorized area.",
                Optional.of(NotificationRoutePolicy.Destination.ATTENDANCE),
                NotificationRoutePolicy.destination(
                        NotificationRoute.ATTENDANCE,
                        Set.of(MobileArea.CUSTOMER_SERVICE)));
        assertTrue(
                "A push must not bypass an absent shell capability.",
                NotificationRoutePolicy.destination(
                                NotificationRoute.ORDERS,
                                Set.of(MobileArea.CATALOG))
                        .isEmpty());
    }

    @Test
    public void prefersPersonalCommissionAndFinanceScopes() {
        Set<MobileArea> areas = Set.of(
                MobileArea.COMMISSIONS,
                MobileArea.COMMISSION_TEAM,
                MobileArea.PERSONAL_FINANCE,
                MobileArea.CORPORATE_FINANCE);

        assertEquals(
                "A personal commission route must not escalate to team scope.",
                Optional.of(
                        NotificationRoutePolicy.Destination.PERSONAL_COMMISSIONS),
                NotificationRoutePolicy.destination(
                        NotificationRoute.COMMISSIONS,
                        areas));
        assertEquals(
                "Personal finance must be preferred when both scopes exist.",
                Optional.of(
                        NotificationRoutePolicy.Destination.PERSONAL_FINANCE),
                NotificationRoutePolicy.destination(
                        NotificationRoute.FINANCE,
                        areas));
    }

    @Test
    public void fallsBackToAuthorizedManagerScopes() {
        assertEquals(
                "A manager may enter the authorized team commission area.",
                Optional.of(
                        NotificationRoutePolicy.Destination.TEAM_COMMISSIONS),
                NotificationRoutePolicy.destination(
                        NotificationRoute.COMMISSIONS,
                        Set.of(MobileArea.COMMISSION_TEAM)));
        assertEquals(
                "Corporate finance requires its dedicated destination.",
                Optional.of(
                        NotificationRoutePolicy.Destination.CORPORATE_FINANCE),
                NotificationRoutePolicy.destination(
                        NotificationRoute.FINANCE,
                        Set.of(MobileArea.CORPORATE_FINANCE)));
    }

    @Test
    public void acceptsAnyAuthorizedAgendaVisibilityWithoutChangingScope() {
        for (MobileArea area : Set.of(
                MobileArea.AGENDA,
                MobileArea.TEAM_AGENDA,
                MobileArea.GLOBAL_AGENDA)) {
            assertEquals(
                    "Every authorized agenda visibility shares one safe entry.",
                    Optional.of(
                            NotificationRoutePolicy.Destination.APPOINTMENTS),
                    NotificationRoutePolicy.destination(
                            NotificationRoute.APPOINTMENTS,
                            Set.of(area)));
        }
    }

    @Test
    public void homeRemainsSafeWithoutAnyFeatureCapability() {
        assertEquals(
                "Home is only the already-authorized shell.",
                Optional.of(NotificationRoutePolicy.Destination.HOME),
                NotificationRoutePolicy.destination(
                        NotificationRoute.HOME,
                        Set.of()));
    }
}
