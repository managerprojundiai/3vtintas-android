package br.com.tresvtintas.mobile.feature.accountaccess;

import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import java.util.Objects;
import java.util.concurrent.Executor;

public record ManagedAccountAccessFeatureRuntime(
        ManagedAccountAccessReaderFactory readerFactory,
        ManagedAccountRevocationRepositoryFactory revocationFactory,
        Executor workerExecutor,
        GoogleIdTokenRequester googleIdTokenRequester,
        Runnable sessionRejectionHandler) {
    public ManagedAccountAccessFeatureRuntime {
        Objects.requireNonNull(
                readerFactory,
                "Managed account access reader factory is required.");
        Objects.requireNonNull(
                revocationFactory,
                "Managed revocation factory is required.");
        Objects.requireNonNull(
                workerExecutor,
                "Managed account access worker is required.");
        Objects.requireNonNull(
                googleIdTokenRequester,
                "Managed revocation credential requester is required.");
        Objects.requireNonNull(
                sessionRejectionHandler,
                "Managed account access rejection handler is required.");
    }
}
