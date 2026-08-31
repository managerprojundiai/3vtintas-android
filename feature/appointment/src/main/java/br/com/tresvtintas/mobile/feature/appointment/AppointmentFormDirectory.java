package br.com.tresvtintas.mobile.feature.appointment;

import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsibleController;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsibleQuery;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsibleState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.customer.CustomerListController;
import br.com.tresvtintas.mobile.core.customer.CustomerListState;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

final class AppointmentFormDirectory {
    private static final int PAGE_SIZE = 100;
    private final AppointmentFeatureRuntime runtime;
    private final Executor mainExecutor;
    private final Listener listener;
    private Optional<AppointmentResponsibleController> responsibleController =
            Optional.empty();
    private Optional<CustomerListController> customerController =
            Optional.empty();

    AppointmentFormDirectory(
            AppointmentFeatureRuntime runtime,
            Executor mainExecutor,
            Listener listener) {
        this.runtime = Objects.requireNonNull(
                runtime,
                "Appointment runtime is required.");
        this.mainExecutor = Objects.requireNonNull(
                mainExecutor,
                "Main executor is required.");
        this.listener = Objects.requireNonNull(
                listener,
                "Directory listener is required.");
    }

    void open() {
        if (runtime.scope() != AppointmentScope.SELF) {
            AppointmentResponsibleController controller =
                    new AppointmentResponsibleController(
                            runtime.repository(),
                            runtime.workerExecutor(),
                            mainExecutor);
            responsibleController = Optional.of(controller);
            controller.subscribe(this::responsibles);
            controller.open(new AppointmentResponsibleQuery(
                    runtime.scope(),
                    runtime.organizationId(),
                    Optional.empty(),
                    PAGE_SIZE));
        }
        runtime.customerRepository().ifPresent(repository -> {
            CustomerListController controller = new CustomerListController(
                    repository,
                    runtime.workerExecutor(),
                    mainExecutor);
            customerController = Optional.of(controller);
            controller.subscribe(this::customers);
            controller.open(new CustomerQuery(
                    Optional.empty(),
                    PAGE_SIZE));
        });
    }

    void close() {
        responsibleController.ifPresent(
                AppointmentResponsibleController::close);
        customerController.ifPresent(CustomerListController::close);
        responsibleController = Optional.empty();
        customerController = Optional.empty();
    }

    private void responsibles(AppointmentResponsibleState state) {
        listener.onResponsibleState(state);
        if (state.phase() == AppointmentResponsibleState.Phase.READY
                && state.snapshot().orElseThrow().nextCursor().isPresent()) {
            responsibleController.ifPresent(
                    AppointmentResponsibleController::loadMore);
        }
    }

    private void customers(CustomerListState state) {
        listener.onCustomerState(state);
        if (state.phase() == CustomerListState.Phase.READY
                && state.snapshot().orElseThrow().hasMore()) {
            customerController.ifPresent(CustomerListController::loadMore);
        }
    }

    interface Listener {
        void onResponsibleState(AppointmentResponsibleState state);

        void onCustomerState(CustomerListState state);
    }
}
