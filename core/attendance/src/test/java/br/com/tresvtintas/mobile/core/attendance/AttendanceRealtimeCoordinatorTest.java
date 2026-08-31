package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class AttendanceRealtimeCoordinatorTest {
    private static final String STREAM_OPEN_MESSAGE =
            "Realtime source must open.";
    private ScheduledExecutorService scheduler;
    private FakeSource source;
    private AttendanceRealtimeCoordinator coordinator;

    @Before
    public void setUp() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        source = new FakeSource();
        coordinator = new AttendanceRealtimeCoordinator(
                source,
                scheduler,
                Runnable::run,
                () -> 0.5,
                5,
                20,
                15,
                5);
    }

    @After
    public void tearDown() {
        coordinator.close();
        scheduler.shutdownNow();
    }

    @Test
    public void sharesOneForegroundStreamAndDebouncesInvalidations()
            throws Exception {
        Probe first = new Probe();
        Probe second = new Probe();
        try (AttendanceRealtimeSubscription firstHandle =
                     coordinator.observe(first)) {
            assertTrue(
                    STREAM_OPEN_MESSAGE,
                    source.subscribed.await(2, TimeUnit.SECONDS));
            try (AttendanceRealtimeSubscription secondHandle =
                         coordinator.observe(second)) {
                assertEquals(
                        "Visible screens share one stream.",
                        1,
                        source.opens.get());

                source.emit(new AttendanceRealtimeEvent(
                        AttendanceRealtimeEvent.Kind.READY,
                        "10",
                        Instant.parse("2026-07-27T12:00:00Z")));
                source.emit(new AttendanceRealtimeEvent(
                        AttendanceRealtimeEvent.Kind.CHANGE,
                        "11",
                        Instant.parse("2026-07-27T12:00:01Z")));
                source.emit(new AttendanceRealtimeEvent(
                        AttendanceRealtimeEvent.Kind.CHANGE,
                        "12",
                        Instant.parse("2026-07-27T12:00:02Z")));

                assertTrue(
                        "First observer must receive invalidation.",
                        first.invalidated.await(2, TimeUnit.SECONDS));
                assertTrue(
                        "Second observer must receive invalidation.",
                        second.invalidated.await(2, TimeUnit.SECONDS));
                assertEquals(
                        "Burst must coalesce to one refresh.",
                        1,
                        first.changes.get());
                assertEquals(
                        "Realtime state must be connected.",
                        AttendanceRealtimeState.Phase.CONNECTED,
                        coordinator.currentState().phase());

                firstHandle.close();
                assertEquals(
                        "Second observer keeps the stream.",
                        0,
                        source.closes.get());
                secondHandle.close();
                assertEquals(
                        "Last foreground observer closes it.",
                        1,
                        source.closes.get());
                assertEquals(
                        "Realtime state must become idle.",
                        AttendanceRealtimeState.Phase.IDLE,
                        coordinator.currentState().phase());
            }
        }
    }

    @Test
    public void resumesAfterTransientFailureAndPollsWhileDegraded()
            throws Exception {
        Probe probe = new Probe();
        try (AttendanceRealtimeSubscription ignored =
                     coordinator.observe(probe)) {
            assertTrue(
                    STREAM_OPEN_MESSAGE,
                    source.subscribed.await(2, TimeUnit.SECONDS));
            source.emit(new AttendanceRealtimeEvent(
                    AttendanceRealtimeEvent.Kind.READY,
                    "40",
                    Instant.parse("2026-07-27T12:00:00Z")));

            source.fail(AttendanceFailureKind.NETWORK);
            assertTrue(
                    "Fallback state must be visible.",
                    probe.fallback.await(2, TimeUnit.SECONDS));
            assertTrue(
                    "Fallback polling must invalidate data.",
                    probe.invalidated.await(2, TimeUnit.SECONDS));
            waitUntil(() -> source.opens.get() >= 2);
            assertEquals(
                    "Reconnect must resume from memory only.",
                    Optional.of("40"),
                    source.cursors.get(1));
        }
    }

    @Test
    public void doesNotReconnectTerminalProtocolOrAuthorizationFailures()
            throws Exception {
        Probe probe = new Probe();
        try (AttendanceRealtimeSubscription ignored =
                     coordinator.observe(probe)) {
            assertTrue(
                    STREAM_OPEN_MESSAGE,
                    source.subscribed.await(2, TimeUnit.SECONDS));

            source.fail(AttendanceFailureKind.FORBIDDEN);
            Thread.sleep(50);

            assertEquals(
                    "Forbidden stream must fail closed.",
                    1,
                    source.opens.get());
            assertEquals(
                    "Forbidden stream must enter fallback.",
                    AttendanceRealtimeState.Phase.FALLBACK,
                    coordinator.currentState().phase());
            assertEquals(
                    "Forbidden failure must stay observable.",
                    Optional.of(AttendanceFailureKind.FORBIDDEN),
                    coordinator.currentState().failure());
            assertEquals(
                    "Terminal failures must not trigger fallback polling.",
                    0,
                    probe.changes.get());
        }
    }

    @Test
    public void ignoresCallbacksFromSupersededConnections()
            throws Exception {
        Probe probe = new Probe();
        try (AttendanceRealtimeSubscription ignored =
                     coordinator.observe(probe)) {
            assertTrue(
                    STREAM_OPEN_MESSAGE,
                    source.subscribed.await(2, TimeUnit.SECONDS));
            source.emit(new AttendanceRealtimeEvent(
                    AttendanceRealtimeEvent.Kind.READY,
                    "80",
                    Instant.parse("2026-07-27T12:00:00Z")));
            source.fail(AttendanceFailureKind.NETWORK);
            waitUntil(() -> source.opens.get() >= 2);
            source.emit(new AttendanceRealtimeEvent(
                    AttendanceRealtimeEvent.Kind.READY,
                    "80",
                    Instant.parse("2026-07-27T12:00:01Z")));

            source.failAt(0, AttendanceFailureKind.NETWORK);
            Thread.sleep(50);

            assertEquals(
                    "A stale callback must not open another stream.",
                    2,
                    source.opens.get());
            assertEquals(
                    "The replacement stream must remain connected.",
                    AttendanceRealtimeState.Phase.CONNECTED,
                    coordinator.currentState().phase());
        }
    }

    private static void waitUntil(Check check) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!check.value() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue("Expected asynchronous condition.", check.value());
    }

    @FunctionalInterface
    private interface Check {
        boolean value();
    }

    private static final class Probe
            implements AttendanceRealtimeCoordinator.Listener {
        private final CountDownLatch invalidated = new CountDownLatch(1);
        private final CountDownLatch fallback = new CountDownLatch(1);
        private final AtomicInteger changes = new AtomicInteger();

        @Override
        public void onRealtimeStateChanged(
                AttendanceRealtimeState state) {
            if (state.phase()
                    == AttendanceRealtimeState.Phase.FALLBACK) {
                fallback.countDown();
            }
        }

        @Override
        public void onAttendanceInvalidated() {
            changes.incrementAndGet();
            invalidated.countDown();
        }
    }

    private static final class FakeSource
            implements AttendanceRealtimeSource {
        private final CountDownLatch subscribed = new CountDownLatch(1);
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
        private final List<Optional<String>> cursors =
                new CopyOnWriteArrayList<>();
        private final List<Listener> listeners =
                new CopyOnWriteArrayList<>();
        private volatile Listener listener;

        @Override
        public AttendanceRealtimeSubscription subscribe(
                Optional<String> cursor,
                Listener value) {
            cursors.add(cursor);
            listener = value;
            listeners.add(value);
            opens.incrementAndGet();
            subscribed.countDown();
            return closes::incrementAndGet;
        }

        private void emit(AttendanceRealtimeEvent event) {
            listener.onEvent(event);
        }

        private void fail(AttendanceFailureKind kind) {
            listener.onFailure(new AttendanceException(
                    kind,
                    "Synthetic failure."));
        }

        private void failAt(int index, AttendanceFailureKind kind) {
            listeners.get(index).onFailure(new AttendanceException(
                    kind,
                    "Synthetic stale failure."));
        }
    }
}
