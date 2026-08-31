package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Optional;

public record DashboardWorkload(
        Optional<Orders> orders,
        Optional<Quotes> quotes,
        Optional<Deliveries> deliveries) {

    public DashboardWorkload {
        orders = optional(orders);
        quotes = optional(quotes);
        deliveries = optional(deliveries);
    }

    public record Orders(int total, int active, int awaitingPayment) {
        public Orders {
            nonNegative(total, active, awaitingPayment);
        }
    }

    public record Quotes(
            int activeMaterial,
            int activeLabor,
            int expiringSoon) {
        public Quotes {
            nonNegative(activeMaterial, activeLabor, expiringSoon);
        }
    }

    public record Deliveries(
            int active,
            int today,
            int inTransit,
            Optional<Integer> awaitingSchedule) {
        public Deliveries {
            awaitingSchedule = optional(awaitingSchedule);
            nonNegative(active, today, inTransit);
            awaitingSchedule.ifPresent(DashboardWorkload::nonNegative);
        }
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    private static void nonNegative(int... values) {
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Dashboard workload count is invalid.");
            }
        }
    }
}
