package br.com.tresvtintas.mobile.feature.organizationadmin;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record OrganizationAdministrationFeatureRuntime(
        OrganizationAdministrationRepository repository,
        Executor workerExecutor,
        long actorUserId) {
    private static final long MINIMUM_ACTOR_ID = 1L;

    public OrganizationAdministrationFeatureRuntime {
        Objects.requireNonNull(repository, "Organization repository is required.");
        Objects.requireNonNull(workerExecutor, "Organization worker is required.");
        if (actorUserId < MINIMUM_ACTOR_ID) {
            throw new IllegalArgumentException(
                    "Organization administration actor is invalid.");
        }
    }
}
