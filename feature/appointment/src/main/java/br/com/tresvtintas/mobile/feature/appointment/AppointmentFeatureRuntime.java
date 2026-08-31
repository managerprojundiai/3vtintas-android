package br.com.tresvtintas.mobile.feature.appointment;

import br.com.tresvtintas.mobile.core.appointment.AppointmentRepository;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.finance.FinanceRepository;
import br.com.tresvtintas.mobile.feature.finance.FinanceRoute;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

public record AppointmentFeatureRuntime(
        AppointmentRepository repository,
        Executor workerExecutor,
        AppointmentScope scope,
        OptionalLong organizationId,
        Optional<CustomerRepository> customerRepository,
        Optional<FinanceRepository> financeRepository,
        Optional<FinanceRoute> financeRoute,
        Optional<DeliveryRepository> deliveryRepository,
        boolean canWrite) {
    public AppointmentFeatureRuntime {
        Objects.requireNonNull(repository, "Appointment repository is required.");
        Objects.requireNonNull(workerExecutor, "Appointment worker is required.");
        Objects.requireNonNull(scope, "Appointment scope is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        customerRepository = customerRepository == null
                ? Optional.empty()
                : customerRepository;
        financeRepository = financeRepository == null
                ? Optional.empty()
                : financeRepository;
        financeRoute = financeRoute == null
                ? Optional.empty()
                : financeRoute;
        deliveryRepository = deliveryRepository == null
                ? Optional.empty()
                : deliveryRepository;
        if (organizationId.isPresent()
                && organizationId.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "Appointment organization is invalid.");
        }
        if (financeRepository.isPresent() != financeRoute.isPresent()) {
            throw new IllegalArgumentException(
                    "Agenda finance runtime is incomplete.");
        }
    }
}
