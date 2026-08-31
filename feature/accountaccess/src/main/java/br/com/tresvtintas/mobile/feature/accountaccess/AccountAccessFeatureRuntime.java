package br.com.tresvtintas.mobile.feature.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record AccountAccessFeatureRuntime(
        AccountAccessRepository repository,
        Executor workerExecutor,
        Runnable sessionRejectionHandler) {
    public AccountAccessFeatureRuntime {
        Objects.requireNonNull(
                repository,
                "Account access repository is required.");
        Objects.requireNonNull(
                workerExecutor,
                "Account access worker is required.");
        Objects.requireNonNull(
                sessionRejectionHandler,
                "Account access rejection handler is required.");
    }
}
