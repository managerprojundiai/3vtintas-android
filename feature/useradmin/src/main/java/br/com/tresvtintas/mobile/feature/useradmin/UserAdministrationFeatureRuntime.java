package br.com.tresvtintas.mobile.feature.useradmin;

import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

public record UserAdministrationFeatureRuntime(
        UserAdministrationRepository repository,
        Executor workerExecutor,
        long actorUserId,
        Optional<UserSecurityAccessNavigator> securityNavigator) {
    private static final long MINIMUM_USER_ID = 1L;

    public UserAdministrationFeatureRuntime {
        Objects.requireNonNull(repository, "User repository is required.");
        Objects.requireNonNull(workerExecutor, "User worker is required.");
        if (actorUserId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException("Actor user ID is invalid.");
        }
        securityNavigator = Objects.requireNonNull(
                securityNavigator,
                "User security navigator state is required.");
    }
}
