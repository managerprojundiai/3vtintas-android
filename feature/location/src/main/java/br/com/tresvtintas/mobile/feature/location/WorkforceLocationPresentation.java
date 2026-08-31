package br.com.tresvtintas.mobile.feature.location;

final class WorkforceLocationPresentation {
    private static final String STATE_ACTIVE = "active";
    private static final String STATE_BLOCKED = "blocked";
    private static final String STATE_PAUSED = "paused_outside_schedule";

    private WorkforceLocationPresentation() {
    }

    static int serviceDetail(boolean scheduleActive, String serviceState) {
        if (!scheduleActive || STATE_PAUSED.equals(serviceState)) {
            return R.string.location_status_detail_paused;
        }
        if (STATE_ACTIVE.equals(serviceState)) {
            return R.string.location_status_detail_active;
        }
        if (STATE_BLOCKED.equals(serviceState)) {
            return R.string.location_status_detail_blocked;
        }
        return R.string.location_status_detail_starting;
    }
}
