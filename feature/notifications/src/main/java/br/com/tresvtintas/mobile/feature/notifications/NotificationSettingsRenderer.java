package br.com.tresvtintas.mobile.feature.notifications;

import android.view.View;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationFailureKind;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationPreferences;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsState;
import br.com.tresvtintas.mobile.feature.notifications.databinding.NotificationSettingsActivityBinding;
import java.util.EnumMap;
import java.util.Map;

final class NotificationSettingsRenderer {
    private final NotificationSettingsActivityBinding binding;

    NotificationSettingsRenderer(
            NotificationSettingsActivityBinding binding) {
        this.binding = binding;
    }

    void render(NotificationSettingsState state, boolean pushAvailable) {
        boolean busy = state.phase() == NotificationSettingsState.Phase.LOADING
                || state.phase() == NotificationSettingsState.Phase.SAVING;
        binding.notificationProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.notificationContent.setVisibility(
                state.preferences().isPresent() ? View.VISIBLE : View.GONE);
        binding.notificationErrorGroup.setVisibility(
                state.phase() == NotificationSettingsState.Phase.ERROR
                        ? View.VISIBLE
                        : View.GONE);
        binding.notificationSave.setEnabled(!busy && state.preferences().isPresent());
        binding.notificationPermissionAction.setEnabled(!busy);
        binding.notificationTransportStatus.setText(pushAvailable
                ? R.string.notification_transport_ready
                : R.string.notification_transport_configuration);
        state.preferences().ifPresent(this::preferences);
        state.failure().ifPresentOrElse(
                this::notice,
                () -> binding.notificationNotice.setVisibility(View.GONE));
        state.requestId().ifPresentOrElse(
                requestId -> {
                    binding.notificationSupportCode.setText(
                            binding.getRoot().getContext().getString(
                                    R.string.notification_support_code,
                                    requestId));
                    binding.notificationSupportCode.setVisibility(View.VISIBLE);
                },
                () -> binding.notificationSupportCode.setVisibility(View.GONE));
    }

    Map<NotificationCategory, Boolean> selection() {
        Map<NotificationCategory, Boolean> result =
                new EnumMap<>(NotificationCategory.class);
        result.put(
                NotificationCategory.ATTENDANCE,
                binding.notificationAttendance.isChecked());
        result.put(
                NotificationCategory.ORDERS,
                binding.notificationOrders.isChecked());
        result.put(
                NotificationCategory.DELIVERIES,
                binding.notificationDeliveries.isChecked());
        result.put(
                NotificationCategory.QUOTES,
                binding.notificationQuotes.isChecked());
        result.put(
                NotificationCategory.COMMISSIONS,
                binding.notificationCommissions.isChecked());
        result.put(
                NotificationCategory.APPOINTMENTS,
                binding.notificationAppointments.isChecked());
        result.put(
                NotificationCategory.FINANCE,
                binding.notificationFinance.isChecked());
        result.put(
                NotificationCategory.AGENT,
                binding.notificationAgent.isChecked());
        return result;
    }

    boolean operationalEnabled() {
        return binding.notificationOperational.isChecked();
    }

    private void preferences(NotificationPreferences value) {
        binding.notificationOperational.setChecked(value.operationalEnabled());
        binding.notificationAttendance.setChecked(
                value.enabled(NotificationCategory.ATTENDANCE));
        binding.notificationOrders.setChecked(
                value.enabled(NotificationCategory.ORDERS));
        binding.notificationDeliveries.setChecked(
                value.enabled(NotificationCategory.DELIVERIES));
        binding.notificationQuotes.setChecked(
                value.enabled(NotificationCategory.QUOTES));
        binding.notificationCommissions.setChecked(
                value.enabled(NotificationCategory.COMMISSIONS));
        binding.notificationAppointments.setChecked(
                value.enabled(NotificationCategory.APPOINTMENTS));
        binding.notificationFinance.setChecked(
                value.enabled(NotificationCategory.FINANCE));
        binding.notificationAgent.setChecked(
                value.enabled(NotificationCategory.AGENT));
        binding.notificationSecurity.setChecked(value.essentialSecurityAlerts());
        int permissionText = switch (value.permissionState()) {
            case UNKNOWN -> R.string.notification_permission_unknown;
            case GRANTED -> R.string.notification_permission_granted;
            case DENIED -> R.string.notification_permission_denied;
        };
        binding.notificationPermissionStatus.setText(permissionText);
        binding.notificationPermissionAction.setText(
                value.permissionState() == NotificationPermissionState.GRANTED
                        ? R.string.notification_permission_settings
                        : R.string.notification_permission_enable);
    }

    private void notice(NotificationFailureKind failure) {
        int message = switch (failure) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.notification_error_session;
            case CONFIGURATION ->
                R.string.notification_error_configuration;
            case CONFLICT ->
                R.string.notification_error_conflict;
            case NETWORK ->
                R.string.notification_error_network;
            case RATE_LIMITED ->
                R.string.notification_error_rate;
            case UPDATE_REQUIRED ->
                R.string.notification_error_update;
            case SERVICE_UNAVAILABLE ->
                R.string.notification_error_service;
            case INVALID_REQUEST, PROTOCOL ->
                R.string.notification_error_generic;
        };
        binding.notificationNotice.setText(message);
        binding.notificationNotice.setVisibility(View.VISIBLE);
    }
}
