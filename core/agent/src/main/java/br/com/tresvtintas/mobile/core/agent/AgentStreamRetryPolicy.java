package br.com.tresvtintas.mobile.core.agent;

import java.util.function.DoubleSupplier;

final class AgentStreamRetryPolicy {
    private static final long BASE_MILLIS = 1_000;
    private static final long MAXIMUM_MILLIS = 30_000;
    private final DoubleSupplier jitter;

    AgentStreamRetryPolicy(DoubleSupplier jitter) {
        if (jitter == null) {
            throw new IllegalArgumentException(
                    "Agent stream jitter is required.");
        }
        this.jitter = jitter;
    }

    long delay(int attempt) {
        int boundedAttempt = Math.max(1, Math.min(attempt, 30));
        long exponential = BASE_MILLIS;
        for (int index = 1;
                index < boundedAttempt
                        && exponential < MAXIMUM_MILLIS;
                index++) {
            exponential = Math.min(
                    MAXIMUM_MILLIS,
                    exponential * 2);
        }
        double sample = jitter.getAsDouble();
        double bounded = Double.isFinite(sample)
                ? Math.max(0, Math.min(1, sample))
                : 0.5;
        double factor = 0.8 + bounded * 0.4;
        return Math.max(
                1,
                Math.min(
                        MAXIMUM_MILLIS,
                        Math.round(exponential * factor)));
    }
}
