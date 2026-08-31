package br.com.tresvtintas.mobile.core.systemconfiguration;

import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;

public interface SystemConfigurationRepository {
    Snapshot load() throws SystemConfigurationException;

    Mutation update(
            Values values,
            int expectedRevision,
            String idempotencyKey) throws SystemConfigurationException;
}
