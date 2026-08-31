package br.com.tresvtintas.mobile.core.attendance;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

/**
 * Session-scoped realtime lifecycle. It opens one stream only while at least one Attendance
 * screen is visible, keeps the cursor in memory, reconnects with bounded jitter, and emits a
 * foreground-only polling fallback.
 */
public final class AttendanceRealtimeCoordinator implements AutoCloseable {
    public interface Listener {
        void onRealtimeStateChanged(AttendanceRealtimeState state);

        void onAttendanceInvalidated();
    }

    private static final long RETRY_BASE_MILLIS = 1_000;
    private static final long RETRY_MAX_MILLIS = 30_000;
    private static final long FALLBACK_POLL_MILLIS = 60_000;
    private static final long INVALIDATION_DEBOUNCE_MILLIS = 250;
    private final Object lock = new Object();
    private final AttendanceRealtimeSource source;
    private final ScheduledExecutorService scheduler;
    private final Executor callbackExecutor;
    private final DoubleSupplier jitter;
    private final long retryBaseMillis;
    private final long retryMaxMillis;
    private final long fallbackPollMillis;
    private final long invalidationDebounceMillis;
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private AttendanceRealtimeState state =
            AttendanceRealtimeState.idle();
    private Optional<String> cursor = Optional.empty();
    private Optional<AttendanceRealtimeSubscription> active =
            Optional.empty();
    private Optional<ScheduledFuture<?>> reconnect = Optional.empty();
    private Optional<ScheduledFuture<?>> fallback = Optional.empty();
    private Optional<ScheduledFuture<?>> invalidation = Optional.empty();
    private long generation;
    private long connectionGeneration;
    private int attempt;
    private boolean closed;

    public AttendanceRealtimeCoordinator(
            AttendanceRealtimeSource source,
            ScheduledExecutorService scheduler,
            Executor callbackExecutor) {
        this(
                source,
                scheduler,
                callbackExecutor,
                Math::random,
                RETRY_BASE_MILLIS,
                RETRY_MAX_MILLIS,
                FALLBACK_POLL_MILLIS,
                INVALIDATION_DEBOUNCE_MILLIS);
    }

    AttendanceRealtimeCoordinator(
            AttendanceRealtimeSource source,
            ScheduledExecutorService scheduler,
            Executor callbackExecutor,
            DoubleSupplier jitter,
            long retryBaseMillis,
            long retryMaxMillis,
            long fallbackPollMillis,
            long invalidationDebounceMillis) {
        this.source = Objects.requireNonNull(
                source,
                "Realtime source is required.");
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "Realtime scheduler is required.");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "Realtime callback executor is required.");
        this.jitter = Objects.requireNonNull(
                jitter,
                "Realtime jitter source is required.");
        this.retryBaseMillis = positive(
                retryBaseMillis,
                "Realtime retry base is invalid.");
        this.retryMaxMillis = positive(
                retryMaxMillis,
                "Realtime retry maximum is invalid.");
        this.fallbackPollMillis = positive(
                fallbackPollMillis,
                "Realtime fallback interval is invalid.");
        this.invalidationDebounceMillis = positive(
                invalidationDebounceMillis,
                "Realtime debounce is invalid.");
        if (retryBaseMillis > retryMaxMillis) {
            throw new IllegalArgumentException(
                    "Realtime retry range is invalid.");
        }
    }

    public AttendanceRealtimeSubscription observe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Realtime listener is required.");
        AttendanceRealtimeState snapshot;
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException(
                        "Realtime coordinator is closed.");
            }
            boolean start = listeners.isEmpty();
            listeners.add(required);
            if (start) {
                state = AttendanceRealtimeState.connecting();
                generation++;
                scheduleConnectLocked(generation, 0);
            }
            snapshot = state;
        }
        callbackExecutor.execute(() ->
                required.onRealtimeStateChanged(snapshot));
        return () -> remove(required);
    }

    public AttendanceRealtimeState currentState() {
        synchronized (lock) {
            return state;
        }
    }

    @Override
    public void close() {
        Set<Listener> snapshot;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            generation++;
            cancelLifecycleLocked();
            state = AttendanceRealtimeState.closed();
            snapshot = new LinkedHashSet<>(listeners);
            listeners.clear();
        }
        publishState(snapshot, AttendanceRealtimeState.closed());
    }

    private void remove(Listener listener) {
        synchronized (lock) {
            listeners.remove(listener);
            if (!listeners.isEmpty() || closed) {
                return;
            }
            generation++;
            cancelLifecycleLocked();
            attempt = 0;
            state = AttendanceRealtimeState.idle();
        }
    }

    private void scheduleConnectLocked(long expectedGeneration, long delay) {
        cancel(reconnect);
        reconnect = Optional.of(scheduler.schedule(
                () -> connect(expectedGeneration),
                delay,
                TimeUnit.MILLISECONDS));
    }

    private void connect(long expectedGeneration) {
        Optional<String> resume;
        long expectedConnection;
        synchronized (lock) {
            if (!active(expectedGeneration)) {
                return;
            }
            resume = cursor;
            connectionGeneration++;
            expectedConnection = connectionGeneration;
        }
        try {
            install(
                    expectedGeneration,
                    expectedConnection,
                    source.subscribe(
                            resume,
                            new SourceListener(
                                    expectedGeneration,
                                    expectedConnection)));
        } catch (AttendanceException failure) {
            failed(
                    expectedGeneration,
                    expectedConnection,
                    failure);
        }
    }

    private void install(
            long expectedGeneration,
            long expectedConnection,
            AttendanceRealtimeSubscription subscription) {
        AttendanceRealtimeSubscription required =
                Objects.requireNonNull(
                        subscription,
                        "Realtime subscription is required.");
        synchronized (lock) {
            if (!active(expectedGeneration, expectedConnection)) {
                required.close();
                return;
            }
            active = Optional.of(required);
        }
    }

    private void event(
            long expectedGeneration,
            long expectedConnection,
            AttendanceRealtimeEvent event) {
        Optional<AttendanceRealtimeState> next = Optional.empty();
        Optional<AttendanceRealtimeState> failureState =
                Optional.empty();
        synchronized (lock) {
            if (!active(expectedGeneration, expectedConnection)) {
                return;
            }
            long previous = cursor.map(Long::parseLong).orElse(-1L);
            long received = event.cursorValue();
            boolean validReady =
                    event.kind() == AttendanceRealtimeEvent.Kind.READY
                            && (previous < 0 || received == previous);
            boolean validAdvance =
                    event.kind() != AttendanceRealtimeEvent.Kind.READY
                            && received > previous;
            if (!validReady && !validAdvance) {
                failureState = Optional.of(failedLocked(
                        expectedGeneration,
                        expectedConnection,
                        new AttendanceException(
                                AttendanceFailureKind.PROTOCOL,
                                "Realtime cursor order is invalid.")));
            } else {
                cursor = Optional.of(event.cursor());
                attempt = 0;
                cancel(reconnect);
                reconnect = Optional.empty();
                cancel(fallback);
                fallback = Optional.empty();
                if (state.phase()
                        != AttendanceRealtimeState.Phase.CONNECTED) {
                    state = AttendanceRealtimeState.connected();
                    next = Optional.of(state);
                }
                if (event.kind() == AttendanceRealtimeEvent.Kind.CHANGE) {
                    scheduleInvalidationLocked(expectedGeneration);
                }
            }
        }
        failureState.ifPresent(this::publishState);
        next.ifPresent(this::publishState);
    }

    private void failed(
            long expectedGeneration,
            long expectedConnection,
            AttendanceException failure) {
        AttendanceRealtimeState next;
        synchronized (lock) {
            if (!active(expectedGeneration, expectedConnection)) {
                return;
            }
            next = failedLocked(
                    expectedGeneration,
                    expectedConnection,
                    failure);
        }
        publishState(next);
    }

    private AttendanceRealtimeState failedLocked(
            long expectedGeneration,
            long expectedConnection,
            AttendanceException failure) {
        if (!active(expectedGeneration, expectedConnection)) {
            return state;
        }
        connectionGeneration++;
        active.ifPresent(AttendanceRealtimeSubscription::close);
        active = Optional.empty();
        state = AttendanceRealtimeState.fallback(failure.kind());
        if (!terminal(failure.kind())) {
            startFallbackLocked(expectedGeneration);
            attempt = Math.min(attempt + 1, 30);
            scheduleConnectLocked(
                    expectedGeneration,
                    retryDelay(attempt));
        } else {
            cancel(fallback);
            fallback = Optional.empty();
        }
        return state;
    }

    private void streamClosed(
            long expectedGeneration,
            long expectedConnection) {
        failed(
                expectedGeneration,
                expectedConnection,
                new AttendanceException(
                        AttendanceFailureKind.NETWORK,
                        "Realtime stream closed."));
    }

    private void startFallbackLocked(long expectedGeneration) {
        if (fallback.filter(value -> !value.isDone()).isPresent()) {
            return;
        }
        fallback = Optional.of(scheduler.scheduleAtFixedRate(
                () -> publishInvalidation(expectedGeneration),
                fallbackPollMillis,
                fallbackPollMillis,
                TimeUnit.MILLISECONDS));
    }

    private void scheduleInvalidationLocked(long expectedGeneration) {
        if (invalidation.filter(value -> !value.isDone()).isPresent()) {
            return;
        }
        invalidation = Optional.of(scheduler.schedule(
                () -> publishInvalidation(expectedGeneration),
                invalidationDebounceMillis,
                TimeUnit.MILLISECONDS));
    }

    private void publishInvalidation(long expectedGeneration) {
        Set<Listener> snapshot;
        synchronized (lock) {
            if (!active(expectedGeneration)) {
                return;
            }
            snapshot = new LinkedHashSet<>(listeners);
        }
        callbackExecutor.execute(() -> snapshot.forEach(
                Listener::onAttendanceInvalidated));
    }

    private long retryDelay(int retryAttempt) {
        long exponential = retryBaseMillis;
        for (int index = 1;
                index < retryAttempt && exponential < retryMaxMillis;
                index++) {
            exponential = Math.min(
                    retryMaxMillis,
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
                        retryMaxMillis,
                        Math.round(exponential * factor)));
    }

    private void publishState(AttendanceRealtimeState next) {
        Set<Listener> snapshot;
        synchronized (lock) {
            snapshot = new LinkedHashSet<>(listeners);
        }
        publishState(snapshot, next);
    }

    private void publishState(
            Set<Listener> snapshot,
            AttendanceRealtimeState next) {
        callbackExecutor.execute(() -> snapshot.forEach(listener ->
                listener.onRealtimeStateChanged(next)));
    }

    private boolean active(long expectedGeneration) {
        return !closed
                && !listeners.isEmpty()
                && generation == expectedGeneration;
    }

    private boolean active(
            long expectedGeneration,
            long expectedConnection) {
        return active(expectedGeneration)
                && connectionGeneration == expectedConnection;
    }

    private void cancelLifecycleLocked() {
        connectionGeneration++;
        active.ifPresent(AttendanceRealtimeSubscription::close);
        active = Optional.empty();
        cancel(reconnect);
        cancel(fallback);
        cancel(invalidation);
        reconnect = Optional.empty();
        fallback = Optional.empty();
        invalidation = Optional.empty();
    }

    private static void cancel(
            Optional<ScheduledFuture<?>> future) {
        future.ifPresent(value -> value.cancel(false));
    }

    private static boolean terminal(AttendanceFailureKind kind) {
        return kind == AttendanceFailureKind.ACCESS_REVOKED
                || kind == AttendanceFailureKind.AUTH_REJECTED
                || kind == AttendanceFailureKind.FORBIDDEN
                || kind == AttendanceFailureKind.PROTOCOL
                || kind == AttendanceFailureKind.UPDATE_REQUIRED;
    }

    private static long positive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private final class SourceListener
            implements AttendanceRealtimeSource.Listener {
        private final long expectedGeneration;
        private final long expectedConnection;

        private SourceListener(
                long expectedGeneration,
                long expectedConnection) {
            this.expectedGeneration = expectedGeneration;
            this.expectedConnection = expectedConnection;
        }

        @Override
        public void onEvent(AttendanceRealtimeEvent event) {
            AttendanceRealtimeCoordinator.this.event(
                    expectedGeneration,
                    expectedConnection,
                    Objects.requireNonNull(
                            event,
                            "Realtime event is required."));
        }

        @Override
        public void onFailure(AttendanceException failure) {
            AttendanceRealtimeCoordinator.this.failed(
                    expectedGeneration,
                    expectedConnection,
                    Objects.requireNonNull(
                            failure,
                            "Realtime failure is required."));
        }

        @Override
        public void onClosed() {
            streamClosed(
                    expectedGeneration,
                    expectedConnection);
        }
    }
}
