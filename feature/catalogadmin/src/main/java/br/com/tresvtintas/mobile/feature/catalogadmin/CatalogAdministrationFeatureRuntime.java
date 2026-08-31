package br.com.tresvtintas.mobile.feature.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationRepository;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record CatalogAdministrationFeatureRuntime(
        CatalogAdministrationRepository repository,
        CatalogImportRepository importRepository,
        Executor workerExecutor,
        long actorUserId) {
    private static final long MINIMUM_ACTOR_ID = 1L;

    public CatalogAdministrationFeatureRuntime {
        Objects.requireNonNull(repository, "Catalog repository is required.");
        Objects.requireNonNull(
                importRepository,
                "Catalog import repository is required.");
        Objects.requireNonNull(workerExecutor, "Catalog worker is required.");
        if (actorUserId < MINIMUM_ACTOR_ID) {
            throw new IllegalArgumentException(
                    "Catalog administration actor is invalid.");
        }
    }
}
