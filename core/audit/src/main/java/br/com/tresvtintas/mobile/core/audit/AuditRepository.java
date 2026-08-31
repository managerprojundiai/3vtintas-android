package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import java.util.Optional;

@FunctionalInterface
public interface AuditRepository {
    Page events(AuditQuery query, Optional<String> cursor) throws AuditException;
}
