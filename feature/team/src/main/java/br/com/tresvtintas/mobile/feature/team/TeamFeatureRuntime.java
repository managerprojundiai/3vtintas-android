package br.com.tresvtintas.mobile.feature.team;

import br.com.tresvtintas.mobile.core.team.TeamRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record TeamFeatureRuntime(
        TeamRepository repository,
        Executor workerExecutor) {
    public TeamFeatureRuntime {
        Objects.requireNonNull(repository, "Team repository is required.");
        Objects.requireNonNull(workerExecutor, "Team worker is required.");
    }
}
