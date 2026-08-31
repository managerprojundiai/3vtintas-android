package br.com.tresvtintas.mobile.data.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeEvent;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeSource;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeSubscription;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAttendanceEventSourceTest {
    private static final int SIGNAL_TIMEOUT_SECONDS = 15;
    private MockWebServer server;
    private RemoteAttendanceEventSource source;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        source = new RemoteAttendanceEventSource(
                new AttendanceAccountScope(
                        41,
                        "b".repeat(64),
                        OptionalLong.of(7)),
                MobileApiFactory.createClient(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.25.0-attendance-stream",
                                26,
                                true),
                        () -> Optional.of("a".repeat(80)),
                        null));
    }

    @After
    public void tearDown() throws IOException {
        source.close();
        server.shutdown();
    }

    @Test
    public void readsStrictMinimalFramesAndSendsResumeCursor()
            throws Exception {
        server.enqueue(streamResponse(
                "retry: 1000\n"
                        + "id: 50\n"
                        + "event: attendance.ready\n"
                        + "data: {\"kind\":\"ready\",\"cursor\":\"50\","
                        + "\"occurredAt\":\"2026-07-27T12:00:00Z\"}\n\n"
                        + ": heartbeat\n\n"
                        + "id: 52\n"
                        + "event: attendance.change\n"
                        + "data: {\"kind\":\"change\",\"cursor\":\"52\","
                        + "\"occurredAt\":\"2026-07-27T12:00:01Z\"}\n\n"));
        Probe probe = new Probe(2);

        try (AttendanceRealtimeSubscription ignored =
                     source.subscribe(Optional.of("49"), probe)) {
            assertTrue("Two event frames must arrive.", probe.await());
            RecordedRequest request = server.takeRequest();
            assertEquals(
                    "Resume cursor must be sent.",
                    "49",
                    request.getHeader("Last-Event-ID"));
            assertEquals(
                    "SSE content type must be requested.",
                    "text/event-stream",
                    request.getHeader("Accept"));
            assertEquals(
                    "Bearer credential must be attached.",
                    "Bearer " + "a".repeat(80),
                    request.getHeader("Authorization"));
            assertEquals(
                    "Only the expected minimal event kinds may arrive.",
                    List.of(
                            AttendanceRealtimeEvent.Kind.READY,
                            AttendanceRealtimeEvent.Kind.CHANGE),
                    probe.events.stream()
                            .map(AttendanceRealtimeEvent::kind)
                            .toList());
            assertEquals(
                    "Newest cursor must be retained.",
                    "52",
                    probe.events.get(1).cursor());
        }
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void rejectsUnknownContentAndNonMonotonicFrames()
            throws Exception {
        for (String second : List.of(
                "id: 51\n"
                        + "event: attendance.change\n"
                        + "data: {\"kind\":\"change\",\"cursor\":\"51\","
                        + "\"occurredAt\":\"2026-07-27T12:00:01Z\","
                        + "\"content\":\"secret\"}\n\n",
                "id: 50\n"
                        + "event: attendance.change\n"
                        + "data: {\"kind\":\"change\",\"cursor\":\"50\","
                        + "\"occurredAt\":\"2026-07-27T12:00:01Z\"}\n\n")) {
            server.enqueue(streamResponse(
                    "id: 50\n"
                            + "event: attendance.ready\n"
                            + "data: {\"kind\":\"ready\",\"cursor\":\"50\","
                            + "\"occurredAt\":\"2026-07-27T12:00:00Z\"}\n\n"
                            + second));
            Probe probe = new Probe(2);
            try (AttendanceRealtimeSubscription ignored =
                         source.subscribe(Optional.empty(), probe)) {
                assertTrue(
                        "Protocol failure must be reported.",
                        probe.await());
                assertEquals(
                        "Malformed frames must map to protocol failure.",
                        AttendanceFailureKind.PROTOCOL,
                        probe.failure.get().kind());
            }
        }
    }

    @Test
    public void mapsRejectedAuthenticationWithoutReadingOrLoggingBody()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader(
                        "Content-Type",
                        "application/problem+json")
                .setBody("{\"detail\":\"must-not-be-surfaced\"}"));
        Probe probe = new Probe(1);

        try (AttendanceRealtimeSubscription ignored =
                     source.subscribe(Optional.empty(), probe)) {
            assertTrue(
                    "Authentication rejection must be reported.",
                    probe.await());
            assertEquals(
                    "HTTP 401 must map to an authentication failure.",
                    AttendanceFailureKind.AUTH_REJECTED,
                    probe.failure.get().kind());
            assertTrue(
                    "Failure message must stay generic.",
                    probe.failure.get().getMessage().contains("rejected"));
        }
    }

    private static MockResponse streamResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader(
                        "Content-Type",
                        "text/event-stream; charset=utf-8")
                .setBody(body);
    }

    private static final class Probe
            implements AttendanceRealtimeSource.Listener {
        private final CountDownLatch latch;
        private final List<AttendanceRealtimeEvent> events =
                new ArrayList<>();
        private final AtomicReference<AttendanceException> failure =
                new AtomicReference<>();

        private Probe(int signals) {
            latch = new CountDownLatch(signals);
        }

        @Override
        public synchronized void onEvent(AttendanceRealtimeEvent event) {
            events.add(event);
            latch.countDown();
        }

        @Override
        public void onFailure(AttendanceException value) {
            failure.set(value);
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }

        @Override
        public void onClosed() {
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }

        private boolean await() throws InterruptedException {
            return latch.await(
                    SIGNAL_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
        }
    }
}
