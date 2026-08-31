package br.com.tresvtintas.mobile.data.systemconfiguration;

import br.com.tresvtintas.mobile.core.network.dto.SystemConfigurationDtos;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import java.time.Instant;

final class SystemConfigurationDtoMapper {
    private SystemConfigurationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Snapshot snapshot(SystemConfigurationDtos.Configuration dto) {
        return new Snapshot(
                new Values(
                        dto.storeName(),
                        dto.storePhone(),
                        dto.storeAddress(),
                        dto.laborCompanyName(),
                        dto.laborCompanyContact(),
                        dto.defaultCommissionRate(),
                        dto.autoApprovePainters()),
                dto.revision(),
                Instant.parse(dto.updatedAt()));
    }

    static Mutation mutation(
            SystemConfigurationDtos.Mutation dto,
            boolean replayed) {
        return new Mutation(
                snapshot(dto.configuration()),
                dto.changed(),
                replayed);
    }

    static SystemConfigurationDtos.UpdateRequest request(
            Values values,
            int revision) {
        return new SystemConfigurationDtos.UpdateRequest(
                values.storeName(),
                values.storePhone(),
                values.storeAddress(),
                values.laborCompanyName(),
                values.laborCompanyContact(),
                values.defaultCommissionRate(),
                values.autoApprovePainters(),
                revision,
                true);
    }
}
