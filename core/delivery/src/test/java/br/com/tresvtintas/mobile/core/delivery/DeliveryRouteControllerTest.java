package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class DeliveryRouteControllerTest {
    @Test
    public void loadsOnlyTheExplicitServiceDate() {
        LocalDate initial = LocalDate.of(2026, 8, 3);
        LocalDate requested = initial.plusDays(1);
        FakeRepository repository = new FakeRepository();
        DeliveryRouteController controller = new DeliveryRouteController(
                repository,
                Runnable::run,
                Runnable::run,
                initial);

        controller.open(requested);
        DeliveryRouteState state = state(controller);

        assertEquals(
                "The selected date must be sent without an inferred role or scope.",
                requested,
                repository.requestedDate);
        assertEquals(
                "A successful empty itinerary must be a ready state.",
                DeliveryRouteState.Phase.READY,
                state.phase());
    }

    private static DeliveryRouteState state(DeliveryRouteController controller) {
        DeliveryRouteState[] state = new DeliveryRouteState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static final class FakeRepository implements DeliveryRouteRepository {
        private LocalDate requestedDate;

        @Override
        public DeliveryRoutePage routes(LocalDate serviceDate) {
            requestedDate = serviceDate;
            return new DeliveryRoutePage(List.of());
        }

        @Override
        public DeliveryRoutePlan route(String routeKey) {
            throw new AssertionError("Not used.");
        }
    }
}
