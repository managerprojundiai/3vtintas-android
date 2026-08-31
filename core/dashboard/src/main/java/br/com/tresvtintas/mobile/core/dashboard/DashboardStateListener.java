package br.com.tresvtintas.mobile.core.dashboard;

@FunctionalInterface
public interface DashboardStateListener {
    void onDashboardStateChanged(DashboardState state);
}
