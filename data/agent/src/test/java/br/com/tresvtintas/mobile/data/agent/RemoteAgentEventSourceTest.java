package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.agent.AgentEvent;
import br.com.tresvtintas.mobile.core.agent.AgentEventKind;
import br.com.tresvtintas.mobile.core.agent.AgentEventSource;
import br.com.tresvtintas.mobile.core.agent.AgentEventSubscription;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAgentEventSourceTest {
    private static final String TURN_ID =
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    private MockWebServer server;
    private RemoteAgentEventSource source;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        source = new RemoteAgentEventSource(
                new AgentAccountScope(41, "b".repeat(64)),
                MobileApiFactory.createClient(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.26.0-agent",
                                27,
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
    public void resumesAndMapsOnlySafeAgentFields()
            throws Exception {
        server.enqueue(stream(
                "retry: 1000\n\n"
                        + frame(
                                4,
                                "started",
                                "{\"status\":\"running\","
                                        + "\"attempt\":2,"
                                        + "\"resetPartialResponse\":true}")
                        + ": heartbeat\n\n"
                        + frame(
                                5,
                                "text_delta",
                                "{\"delta\":\"Preço localizado\"}")
                        + frame(
                                6,
                                "completed",
                                "{\"status\":\"completed\","
                                        + "\"messageId\":44,"
                                        + "\"requiresHuman\":false,"
                                        + "\"blocked\":false}")));
        Probe probe = new Probe(3);

        try (AgentEventSubscription ignored = source.subscribe(
                TURN_ID,
                Optional.of("3"),
                probe)) {
            assertTrue(
                    "Three durable events must arrive.",
                    probe.await());
            RecordedRequest request = server.takeRequest();
            assertEquals(
                    "Last-Event-ID must resume the durable stream.",
                    "3",
                    request.getHeader("Last-Event-ID"));
            assertEquals(
                    "Authenticated stream must request SSE.",
                    "text/event-stream",
                    request.getHeader("Accept"));
            assertTrue(
                    "Worker retry must clear stale partial response.",
                    probe.events.get(0).resetPartialResponse());
            assertEquals(
                    "Only safe text delta is exposed.",
                    Optional.of("Preço localizado"),
                    probe.events.get(1).textDelta());
            assertEquals(
                    "Terminal event must be recognized.",
                    AgentEventKind.COMPLETED,
                    probe.events.get(2).kind());
        }
    }

    @Test
    public void rejectsMismatchedEnvelopeWithoutSurfacingBody()
            throws Exception {
        server.enqueue(stream(frame(
                4,
                "text_delta",
                "{\"delta\":\"segredo\"}")
                .replace("agent.text_delta", "agent.completed")));
        Probe probe = new Probe(1);

        try (AgentEventSubscription ignored = source.subscribe(
                TURN_ID,
                Optional.of("3"),
                probe)) {
            assertTrue(
                    "Protocol rejection must be reported.",
                    probe.await());
            assertEquals(
                    "Envelope mismatch must map to protocol.",
                    AgentFailureKind.PROTOCOL,
                    probe.failure.get().kind());
            assertFalse(
                    "Raw frame content must not enter the error message.",
                    probe.failure.get().getMessage().contains("segredo"));
        }
    }

    private static MockResponse stream(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader(
                        "Content-Type",
                        "text/event-stream; charset=utf-8")
                .setBody(body);
    }

    private static String frame(
            int sequence,
            String kind,
            String payload) {
        return "id: " + sequence + "\n"
                + "event: agent." + kind + "\n"
                + "data: {\"turnId\":\"" + TURN_ID + "\","
                + "\"sequence\":" + sequence + ","
                + "\"kind\":\"" + kind + "\","
                + "\"payload\":" + payload + ","
                + "\"createdAt\":\"2026-07-27T12:00:0"
                + sequence + "Z\"}\n\n";
    }

    private static final class Probe
            implements AgentEventSource.Listener {
        private final CountDownLatch latch;
        private final List<AgentEvent> events = new ArrayList<>();
        private final AtomicReference<AgentException> failure =
                new AtomicReference<>();

        private Probe(int signals) {
            latch = new CountDownLatch(signals);
        }

        @Override
        public synchronized void onEvent(AgentEvent event) {
            events.add(event);
            latch.countDown();
        }

        @Override
        public void onFailure(AgentException value) {
            failure.set(value);
            release();
        }

        @Override
        public void onClosed() {
            release();
        }

        private boolean await() throws InterruptedException {
            return latch.await(5, TimeUnit.SECONDS);
        }

        private void release() {
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }
    }
}
