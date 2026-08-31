package br.com.tresvtintas.mobile.feature.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

public record DeliveryFeatureRuntime(
        DeliveryRepository repository,
        Optional<DeliveryManagementRepository> managementRepository,
        Optional<DeliveryRouteRepository> routeRepository,
        boolean canSchedule,
        boolean canAssign,
        boolean canCompleteManagement,
        Executor workerExecutor) {
    public DeliveryFeatureRuntime {
        Objects.requireNonNull(repository, "Delivery repository is required.");
        managementRepository = Objects.requireNonNull(
                managementRepository,
                "Delivery management repository is required.");
        routeRepository = Objects.requireNonNull(
                routeRepository,
                "Delivery route repository is required.");
        Objects.requireNonNull(workerExecutor, "Delivery worker is required.");
        if (managementRepository.isEmpty()
                && (canSchedule || canAssign || canCompleteManagement)) {
            throw new IllegalArgumentException(
                    "Delivery management access requires a repository.");
        }
    }

    public DeliveryFeatureRuntime(
            DeliveryRepository repository,
            Executor workerExecutor) {
        this(
                repository,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                false,
                workerExecutor);
    }

    public boolean canManage() {
        return managementRepository.isPresent()
                && (canSchedule || canAssign || canCompleteManagement);
    }

    public boolean hasOwnItinerary() {
        return routeRepository.isPresent();
    }
}
