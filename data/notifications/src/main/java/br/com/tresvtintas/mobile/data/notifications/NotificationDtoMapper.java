package br.com.tresvtintas.mobile.data.notifications;

import br.com.tresvtintas.mobile.core.network.dto.NotificationCategoriesDto;
import br.com.tresvtintas.mobile.core.network.dto.NotificationPreferencesDto;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationPreferences;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class NotificationDtoMapper {
    private NotificationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static NotificationPreferences preferences(NotificationPreferencesDto value) {
        return new NotificationPreferences(
                NotificationPermissionState.fromWireValue(
                                value.permissionState())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Notification permission state is unknown.")),
                value.operationalEnabled(),
                categories(value.categories()),
                value.essentialSecurityAlerts(),
                value.revision(),
                Optional.ofNullable(value.updatedAt()).map(Instant::parse));
    }

    static NotificationCategoriesDto categories(
            Map<NotificationCategory, Boolean> values) {
        return new NotificationCategoriesDto(
                enabled(values, NotificationCategory.ATTENDANCE),
                enabled(values, NotificationCategory.ORDERS),
                enabled(values, NotificationCategory.DELIVERIES),
                enabled(values, NotificationCategory.QUOTES),
                enabled(values, NotificationCategory.COMMISSIONS),
                enabled(values, NotificationCategory.APPOINTMENTS),
                enabled(values, NotificationCategory.FINANCE),
                enabled(values, NotificationCategory.AGENT));
    }

    private static Map<NotificationCategory, Boolean> categories(
            NotificationCategoriesDto value) {
        Map<NotificationCategory, Boolean> result =
                new EnumMap<>(NotificationCategory.class);
        result.put(NotificationCategory.ATTENDANCE, value.attendance());
        result.put(NotificationCategory.ORDERS, value.orders());
        result.put(NotificationCategory.DELIVERIES, value.deliveries());
        result.put(NotificationCategory.QUOTES, value.quotes());
        result.put(NotificationCategory.COMMISSIONS, value.commissions());
        result.put(NotificationCategory.APPOINTMENTS, value.appointments());
        result.put(NotificationCategory.FINANCE, value.finance());
        result.put(NotificationCategory.AGENT, value.agent());
        return result;
    }

    private static boolean enabled(
            Map<NotificationCategory, Boolean> values,
            NotificationCategory category) {
        return Boolean.TRUE.equals(values.get(category));
    }
}
