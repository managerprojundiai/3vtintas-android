package br.com.tresvtintas.mobile.feature.commission;

import br.com.tresvtintas.mobile.core.commission.CommissionRepository;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

public record CommissionFeatureRuntime(
        CommissionRepository repository,
        Executor workerExecutor,
        OptionalLong organizationId) {
    public CommissionFeatureRuntime {
        Objects.requireNonNull(repository, "Commission repository is required.");
        Objects.requireNonNull(workerExecutor, "Commission worker is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (organizationId.isPresent()
                && organizationId.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "Commission organization is invalid.");
        }
    }
}
