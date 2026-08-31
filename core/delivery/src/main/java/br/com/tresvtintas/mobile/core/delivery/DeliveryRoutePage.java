package br.com.tresvtintas.mobile.core.delivery;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryRoutePage(List<DeliveryRoutePlan> items) {
    public DeliveryRoutePage {
        Objects.requireNonNull(items, "Delivery routes are required.");
        if (items.size() > 100 || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Delivery route page is invalid.");
        }
        items = List.copyOf(items);
    }

    public Optional<DeliveryRoutePlan> current() {
        return items.stream().min(Comparator
                .comparingInt((DeliveryRoutePlan plan) -> priority(plan.status()))
                .thenComparing(DeliveryRoutePlan::startsAt)
                .reversed());
    }

    private static int priority(DeliveryRouteStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 3;
            case CONFIRMED -> 2;
            case COMPLETED -> 1;
            default -> 0;
        };
    }
}
