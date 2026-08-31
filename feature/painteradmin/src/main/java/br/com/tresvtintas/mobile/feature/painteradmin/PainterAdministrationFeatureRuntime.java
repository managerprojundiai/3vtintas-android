package br.com.tresvtintas.mobile.feature.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record PainterAdministrationFeatureRuntime(
        PainterAdministrationRepository repository,
        Executor workerExecutor) {
    public PainterAdministrationFeatureRuntime {
        Objects.requireNonNull(repository, "Painter repository is required.");
        Objects.requireNonNull(workerExecutor, "Painter worker is required.");
    }
}
