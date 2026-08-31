package br.com.tresvtintas.mobile.core.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public final class NotificationPreferencesTest {
    @Test
    public void securityAlertsRemainEssentialAndOutsideClientSelection() {
        Map<NotificationCategory, Boolean> selected = categories(true);
        selected.put(NotificationCategory.SECURITY, false);

        NotificationPreferences preferences = preferences(selected);

        assertTrue(
                "Essential account security alerts must remain enabled.",
                preferences.enabled(NotificationCategory.SECURITY));
        assertFalse(
                "Security must not be represented as a configurable category.",
                preferences.categories().containsKey(NotificationCategory.SECURITY));
    }

    @Test
    public void createsAnImmutableDefensiveCategorySnapshot() {
        Map<NotificationCategory, Boolean> selected = categories(true);
        NotificationPreferences preferences = preferences(selected);
        selected.put(NotificationCategory.ATTENDANCE, false);

        assertTrue(
                "Later caller mutations must not alter stored preferences.",
                preferences.enabled(NotificationCategory.ATTENDANCE));
        assertThrows(
                "The exposed preference map must be immutable.",
                UnsupportedOperationException.class,
                () -> preferences.categories().put(
                        NotificationCategory.ORDERS,
                        false));
    }

    @Test
    public void rejectsIncompleteOrNonEssentialDocuments() {
        Map<NotificationCategory, Boolean> incomplete = categories(true);
        incomplete.remove(NotificationCategory.FINANCE);

        assertThrows(
                "Every configurable category must be explicit.",
                IllegalArgumentException.class,
                () -> new NotificationPreferences(
                        NotificationPermissionState.GRANTED,
                        true,
                        incomplete,
                        true,
                        1,
                        Optional.empty()));
        assertThrows(
                "The client cannot disable essential security alerts.",
                IllegalArgumentException.class,
                () -> new NotificationPreferences(
                        NotificationPermissionState.GRANTED,
                        true,
                        categories(true),
                        false,
                        1,
                        Optional.empty()));
    }

    @Test
    public void selectionKeepsServerRevisionAndUpdateTimestamp() {
        NotificationPreferences initial = preferences(categories(true));
        Map<NotificationCategory, Boolean> disabled = categories(false);

        NotificationPreferences selected =
                initial.withSelection(false, disabled);

        assertFalse(
                "The operational switch must follow the user selection.",
                selected.operationalEnabled());
        assertFalse(
                "Category selection must be replaced as one document.",
                selected.enabled(NotificationCategory.AGENT));
        assertEquals(
                "Optimistic concurrency revision must be retained.",
                initial.revision(),
                selected.revision());
        assertEquals(
                "The last server timestamp must be retained until save.",
                initial.updatedAt(),
                selected.updatedAt());
    }

    private static NotificationPreferences preferences(
            Map<NotificationCategory, Boolean> selected) {
        return new NotificationPreferences(
                NotificationPermissionState.GRANTED,
                true,
                selected,
                true,
                7,
                Optional.of(Instant.parse("2026-07-28T12:00:00Z")));
    }

    private static Map<NotificationCategory, Boolean> categories(
            boolean enabled) {
        Map<NotificationCategory, Boolean> selected =
                new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable()) {
                selected.put(category, enabled);
            }
        }
        return selected;
    }
}
