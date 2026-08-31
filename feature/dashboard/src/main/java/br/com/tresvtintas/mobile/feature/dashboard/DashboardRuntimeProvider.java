package br.com.tresvtintas.mobile.feature.dashboard;

import java.util.Optional;

@FunctionalInterface
public interface DashboardRuntimeProvider {
    Optional<DashboardFeatureRuntime> dashboardRuntime();
}
