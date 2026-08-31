package br.com.tresvtintas.mobile.feature.whatsappadmin;

import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record WhatsAppAdministrationFeatureRuntime(
        WhatsAppAdministrationRepository repository,
        Executor workerExecutor) {
    public WhatsAppAdministrationFeatureRuntime {
        Objects.requireNonNull(repository, "Repository is required.");
        Objects.requireNonNull(workerExecutor, "Worker executor is required.");
    }
}
