package br.com.tresvtintas.mobile.feature.audit;

import java.util.Optional;

@FunctionalInterface
public interface AuditRuntimeProvider {
    Optional<AuditFeatureRuntime> auditRuntime();
}
