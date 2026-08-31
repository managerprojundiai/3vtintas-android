package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import java.util.Optional;

@FunctionalInterface
public interface AgentReplayRepository {
    Page turns(AgentReplayQuery query, Optional<String> cursor) throws AuditException;
}
