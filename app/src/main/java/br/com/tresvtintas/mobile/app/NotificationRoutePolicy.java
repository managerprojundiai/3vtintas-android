package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.notifications.NotificationRoute;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class NotificationRoutePolicy {
    enum Destination {
        HOME,
        ATTENDANCE,
        ORDERS,
        DELIVERIES,
        MATERIAL_QUOTES,
        PERSONAL_COMMISSIONS,
        TEAM_COMMISSIONS,
        APPOINTMENTS,
        PERSONAL_FINANCE,
        CORPORATE_FINANCE,
        AGENT,
        ACCOUNT_SECURITY
    }

    private NotificationRoutePolicy() {
        throw new AssertionError("No instances.");
    }

    static Optional<Destination> destination(
            NotificationRoute route,
            Set<MobileArea> enabledAreas) {
        NotificationRoute requiredRoute =
                Objects.requireNonNull(route, "Notification route is required.");
        Set<MobileArea> requiredAreas = Objects.requireNonNull(
                enabledAreas,
                "Enabled mobile areas are required.");
        return switch (requiredRoute) {
            case HOME -> Optional.of(Destination.HOME);
            case ATTENDANCE -> whenEnabled(
                    requiredAreas,
                    MobileArea.CUSTOMER_SERVICE,
                    Destination.ATTENDANCE);
            case ORDERS -> whenEnabled(
                    requiredAreas,
                    MobileArea.ORDERS,
                    Destination.ORDERS);
            case DELIVERIES -> whenEnabled(
                    requiredAreas,
                    MobileArea.DELIVERIES,
                    Destination.DELIVERIES);
            case MATERIAL_QUOTES -> whenEnabled(
                    requiredAreas,
                    MobileArea.MATERIAL_QUOTES,
                    Destination.MATERIAL_QUOTES);
            case COMMISSIONS -> firstEnabled(
                    requiredAreas,
                    MobileArea.COMMISSIONS,
                    Destination.PERSONAL_COMMISSIONS,
                    MobileArea.COMMISSION_TEAM,
                    Destination.TEAM_COMMISSIONS);
            case APPOINTMENTS -> appointments(requiredAreas);
            case FINANCE -> firstEnabled(
                    requiredAreas,
                    MobileArea.PERSONAL_FINANCE,
                    Destination.PERSONAL_FINANCE,
                    MobileArea.CORPORATE_FINANCE,
                    Destination.CORPORATE_FINANCE);
            case AGENT -> whenEnabled(
                    requiredAreas,
                    MobileArea.PERSONAL_AI_AGENT,
                    Destination.AGENT);
            case ACCOUNT_SECURITY -> whenEnabled(
                    requiredAreas,
                    MobileArea.ACCOUNT_SECURITY,
                    Destination.ACCOUNT_SECURITY);
        };
    }

    private static Optional<Destination> appointments(
            Set<MobileArea> enabledAreas) {
        return enabledAreas.contains(MobileArea.AGENDA)
                        || enabledAreas.contains(MobileArea.TEAM_AGENDA)
                        || enabledAreas.contains(MobileArea.GLOBAL_AGENDA)
                ? Optional.of(Destination.APPOINTMENTS)
                : Optional.empty();
    }

    private static Optional<Destination> whenEnabled(
            Set<MobileArea> enabledAreas,
            MobileArea area,
            Destination destination) {
        return enabledAreas.contains(area)
                ? Optional.of(destination)
                : Optional.empty();
    }

    private static Optional<Destination> firstEnabled(
            Set<MobileArea> enabledAreas,
            MobileArea primaryArea,
            Destination primaryDestination,
            MobileArea secondaryArea,
            Destination secondaryDestination) {
        if (enabledAreas.contains(primaryArea)) {
            return Optional.of(primaryDestination);
        }
        return whenEnabled(
                enabledAreas,
                secondaryArea,
                secondaryDestination);
    }
}
