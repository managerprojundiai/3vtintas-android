package br.com.tresvtintas.mobile.platform.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationDisplayPolicy;
import br.com.tresvtintas.mobile.core.notifications.NotificationDisplayPolicyStore;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AndroidNotificationDisplayPolicyStore
        implements NotificationDisplayPolicyStore {
    private static final String PREFERENCES_NAME =
            "three_v_notification_display_policy";
    private static final String KEY_SCHEMA_VERSION = "schema_version";
    private static final String KEY_OPERATIONAL_ENABLED = "operational_enabled";
    private static final String KEY_ENABLED_CATEGORIES = "enabled_categories";
    private static final String KEY_ESSENTIAL_SECURITY = "essential_security";
    private static final int SCHEMA_VERSION = 1;
    private static final String CATEGORY_SEPARATOR = ",";

    private final SharedPreferences preferences;

    public AndroidNotificationDisplayPolicyStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required.");
        }
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public Optional<NotificationDisplayPolicy> load() {
        if (preferences.getInt(KEY_SCHEMA_VERSION, 0) != SCHEMA_VERSION
                || !preferences.getBoolean(KEY_ESSENTIAL_SECURITY, false)) {
            return Optional.empty();
        }
        Optional<Set<NotificationCategory>> categories = categories(
                preferences.getString(KEY_ENABLED_CATEGORIES, ""));
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NotificationDisplayPolicy(
                preferences.getBoolean(KEY_OPERATIONAL_ENABLED, false),
                categories.orElseThrow(),
                true));
    }

    @Override
    public void save(NotificationDisplayPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Notification display policy is required.");
        }
        String enabled = policy.enabledCategories().stream()
                .sorted()
                .map(NotificationCategory::wireValue)
                .collect(Collectors.joining(CATEGORY_SEPARATOR));
        preferences.edit()
                .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
                .putBoolean(
                        KEY_OPERATIONAL_ENABLED,
                        policy.operationalEnabled())
                .putString(KEY_ENABLED_CATEGORIES, enabled)
                .putBoolean(
                        KEY_ESSENTIAL_SECURITY,
                        policy.essentialSecurityAlerts())
                .apply();
    }

    @Override
    public void clear() {
        preferences.edit().clear().apply();
    }

    private static Optional<Set<NotificationCategory>> categories(
            String value) {
        Set<NotificationCategory> categories = EnumSet.noneOf(
                NotificationCategory.class);
        if (value == null || value.isBlank()) {
            return Optional.of(categories);
        }
        for (String token : value.split(CATEGORY_SEPARATOR, -1)) {
            Optional<NotificationCategory> category =
                    NotificationCategory.fromWireValue(token);
            if (category.isEmpty() || !category.orElseThrow().configurable()) {
                return Optional.empty();
            }
            categories.add(category.orElseThrow());
        }
        return Optional.of(categories);
    }
}
