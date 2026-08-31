package br.com.tresvtintas.mobile.feature.finance;

import br.com.tresvtintas.mobile.core.finance.FinanceRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record FinanceFeatureRuntime(
        FinanceRepository repository,
        Executor workerExecutor,
        FinanceRoute route) {
    public FinanceFeatureRuntime {
        Objects.requireNonNull(repository, "Finance repository is required.");
        Objects.requireNonNull(workerExecutor, "Finance worker is required.");
        Objects.requireNonNull(route, "Finance route is required.");
    }
}
