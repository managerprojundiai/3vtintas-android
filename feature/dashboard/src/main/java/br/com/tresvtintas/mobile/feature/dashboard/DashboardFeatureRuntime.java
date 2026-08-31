package br.com.tresvtintas.mobile.feature.dashboard;

import br.com.tresvtintas.mobile.core.dashboard.DashboardRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record DashboardFeatureRuntime(
        DashboardRepository repository,
        Executor workerExecutor) {
    public DashboardFeatureRuntime {
        Objects.requireNonNull(repository, "Dashboard repository is required.");
        Objects.requireNonNull(workerExecutor, "Dashboard worker is required.");
    }
}
