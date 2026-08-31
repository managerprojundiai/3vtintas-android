package br.com.tresvtintas.mobile.feature.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayRepository;
import br.com.tresvtintas.mobile.core.audit.AuditRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

public record AuditFeatureRuntime(
        AuditRepository repository,
        Optional<AgentReplayRepository> agentReplayRepository,
        Executor workerExecutor) {
    public AuditFeatureRuntime {
        Objects.requireNonNull(repository, "Audit repository is required.");
        agentReplayRepository = agentReplayRepository == null
                ? Optional.empty()
                : agentReplayRepository;
        Objects.requireNonNull(workerExecutor, "Audit worker is required.");
    }
}
