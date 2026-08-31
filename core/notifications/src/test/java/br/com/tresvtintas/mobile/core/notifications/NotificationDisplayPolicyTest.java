package br.com.tresvtintas.mobile.core.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

public final class NotificationDisplayPolicyTest {
    private static final UUID EVENT_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000091");

    @Test
    public void derivesAuthorizedOperationalCategories() {
        NotificationDisplayPolicy policy = NotificationDisplayPolicy.from(
                preferences(true, false));

        assertTrue("Orders must remain enabled.", policy.allows(message(
                NotificationCategory.ORDERS)));
        assertFalse("Attendance must remain disabled.", policy.allows(message(
                NotificationCategory.ATTENDANCE)));
    }

    @Test
    public void blocksOperationalCategoriesWhenMasterSwitchIsDisabled() {
        NotificationDisplayPolicy policy = NotificationDisplayPolicy.from(
                preferences(false, true));

        assertFalse("The master switch must fail closed.", policy.allows(
                message(NotificationCategory.ORDERS)));
    }

    @Test
    public void preservesEssentialSecurityAlertsIndependently() {
        NotificationDisplayPolicy policy = NotificationDisplayPolicy.from(
                preferences(false, false));

        assertTrue("Security alerts are essential.", policy.allows(message(
                NotificationCategory.SECURITY)));
    }

    @Test
    public void rejectsEssentialCategoryInsideConfigurableSet() {
        assertThrows(
                "Security cannot become user configurable.",
                IllegalArgumentException.class,
                () -> new NotificationDisplayPolicy(
                        true,
                        Set.of(NotificationCategory.SECURITY),
                        true));
    }

    @Test
    public void rejectsMissingMandatorySecurityAlerts() {
        assertThrows(
                "Security alerts must remain mandatory.",
                IllegalArgumentException.class,
                () -> new NotificationDisplayPolicy(true, Set.of(), false));
    }

    @Test
    public void rejectsMissingMessage() {
        NotificationDisplayPolicy policy = NotificationDisplayPolicy.from(
                preferences(true, true));

        assertFalse("A missing payload must fail closed.", policy.allows(null));
    }

    private static NotificationPreferences preferences(
            boolean operationalEnabled,
            boolean attendanceEnabled) {
        Map<NotificationCategory, Boolean> categories =
                new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable()) {
                categories.put(category, category == NotificationCategory.ORDERS
                        || category == NotificationCategory.ATTENDANCE
                        && attendanceEnabled);
            }
        }
        return new NotificationPreferences(
                NotificationPermissionState.GRANTED,
                operationalEnabled,
                categories,
                true,
                1,
                Optional.of(Instant.parse("2026-08-01T00:00:00Z")));
    }

    private static NotificationMessage message(
            NotificationCategory category) {
        return new NotificationMessage(EVENT_ID, category, category.route());
    }
}
