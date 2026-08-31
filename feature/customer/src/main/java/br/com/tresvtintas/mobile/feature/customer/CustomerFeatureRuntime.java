package br.com.tresvtintas.mobile.feature.customer;

import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

public record CustomerFeatureRuntime(
        CustomerRepository repository,
        Executor workerExecutor,
        OptionalLong organizationId,
        boolean writeAllowed) {
    public CustomerFeatureRuntime {
        Objects.requireNonNull(repository, "Customer repository is required.");
        Objects.requireNonNull(workerExecutor, "Customer worker is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (organizationId.isPresent() && organizationId.getAsLong() < 1) {
            throw new IllegalArgumentException(
                    "Customer organization ID is invalid.");
        }
    }
}
