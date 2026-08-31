package br.com.tresvtintas.mobile.data.agent;

import br.com.tresvtintas.mobile.core.agent.AgentEvent;
import br.com.tresvtintas.mobile.core.agent.AgentEventKind;
import br.com.tresvtintas.mobile.core.agent.AgentEventSource;
import br.com.tresvtintas.mobile.core.agent.AgentEventSubscription;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Foreground-only authenticated SSE transport. It exposes only the event fields used by the UI,
 * never logs frames, and keeps the resume cursor in memory.
 */
public final class RemoteAgentEventSource
        implements AgentEventSource, AutoCloseable {
    private static final String STREAM_MEDIA_TYPE = "text/event-stream";
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private static final int MAXIMUM_SEQUENCE = 999_999_999;
    private static final int MAXIMUM_DATA_CHARS = 16_384;
    private static final int MAXIMUM_DELTA_CHARS = 8_000;
    private static final long MAXIMUM_LINE_BYTES = 16_384;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                    + "[1-8][0-9a-fA-F]{3}-"
                    + "[89abAB][0-9a-fA-F]{3}-"
                    + "[0-9a-fA-F]{12}$");
    private final AgentAccountScope scope;
    private final MobileNetworkClient client;
    private final ObjectMapper mapper;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Set<RemoteSubscription> subscriptions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RemoteAgentEventSource(
            AgentAccountScope scope,
            MobileNetworkClient client) {
        this.scope = Objects.requireNonNull(
                scope,
                "Agent event scope is required.");
        this.client = Objects.requireNonNull(
                client,
                "Agent event client is required.");
        mapper = MobileApiFactory.objectMapper();
    }

    public AgentAccountScope scope() {
        return scope;
    }

    @Override
    public AgentEventSubscription subscribe(
            String turnId,
            Optional<String> lastEventId,
            Listener listener) throws AgentException {
        if (!active.get()) {
            throw new AgentException(
                    AgentFailureKind.ACCESS_REVOKED,
                    "Agent event scope is inactive.");
        }
        String requiredTurn = uuid(turnId);
        Optional<String> cursor = requireCursor(lastEventId);
        try {
            Call call = client.newAuthenticatedEventStreamCall(
                    "agent/turns/" + requiredTurn + "/events",
                    cursor);
            RemoteSubscription subscription = new RemoteSubscription(
                    call,
                    requiredTurn,
                    cursor,
                    Objects.requireNonNull(
                            listener,
                            "Agent event listener is required."));
            subscriptions.add(subscription);
            call.enqueue(subscription);
            return subscription;
        } catch (IllegalArgumentException failure) {
            throw new AgentException(
                    AgentFailureKind.PROTOCOL,
                    "Agent event request is invalid.",
                    failure);
        }
    }

    @Override
    public void close() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        subscriptions.forEach(RemoteSubscription::close);
        subscriptions.clear();
    }

    private final class RemoteSubscription
            implements AgentEventSubscription, Callback {
        private final Call call;
        private final String expectedTurn;
        private final Optional<String> initialCursor;
        private final Listener listener;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RemoteSubscription(
                Call call,
                String expectedTurn,
                Optional<String> initialCursor,
                Listener listener) {
            this.call = call;
            this.expectedTurn = expectedTurn;
            this.initialCursor = initialCursor;
            this.listener = listener;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            subscriptions.remove(this);
            call.cancel();
        }

        @Override
        public void onFailure(Call ignored, IOException failure) {
            if (!finish()) {
                return;
            }
            listener.onFailure(new AgentException(
                    failure instanceof AuthenticationRequiredException
                            ? AgentFailureKind.AUTH_REJECTED
                            : AgentFailureKind.NETWORK,
                    "Agent event stream could not be opened.",
                    failure));
        }

        @Override
        public void onResponse(Call ignored, Response response) {
            try (response) {
                if (closed.get()) {
                    return;
                }
                if (response.code() != HTTP_OK) {
                    fail(status(response.code()), response);
                    return;
                }
                if (!streamMediaType(response.body())) {
                    fail(AgentFailureKind.PROTOCOL, response);
                    return;
                }
                read(
                        response.body().source(),
                        expectedTurn,
                        initialCursor,
                        listener);
                if (finish()) {
                    listener.onClosed();
                }
            } catch (IOException failure) {
                if (finish()) {
                    listener.onFailure(new AgentException(
                            AgentFailureKind.NETWORK,
                            "Agent event stream was interrupted.",
                            failure));
                }
            } catch (IllegalArgumentException failure) {
                if (finish()) {
                    listener.onFailure(new AgentException(
                            AgentFailureKind.PROTOCOL,
                            "Agent event stream frame is invalid.",
                            failure));
                }
            }
        }

        private void fail(
                AgentFailureKind kind,
                Response response) {
            if (!finish()) {
                return;
            }
            listener.onFailure(new AgentException(
                    kind,
                    "Agent event stream request was rejected.",
                    requestId(response),
                    null));
        }

        private boolean finish() {
            if (!closed.compareAndSet(false, true)) {
                return false;
            }
            subscriptions.remove(this);
            return true;
        }
    }

    private void read(
            BufferedSource source,
            String expectedTurn,
            Optional<String> initialCursor,
            Listener listener) throws IOException {
        String eventName = "";
        String eventId = "";
        StringBuilder data = new StringBuilder();
        int previous = initialCursor.map(Integer::parseInt).orElse(0);
        while (!source.exhausted()) {
            String line = source.readUtf8LineStrict(MAXIMUM_LINE_BYTES);
            if (line.isEmpty()) {
                if (data.length() > 0) {
                    AgentEvent event = event(
                            eventName,
                            eventId,
                            data.toString(),
                            expectedTurn,
                            previous);
                    listener.onEvent(event);
                    previous = event.sequence();
                }
                eventName = "";
                eventId = "";
                data.setLength(0);
                continue;
            }
            if (line.startsWith(":")) {
                continue;
            }
            int separator = line.indexOf(':');
            String field = separator < 0
                    ? line
                    : line.substring(0, separator);
            String value = separator < 0
                    ? ""
                    : line.substring(separator + 1);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            switch (field) {
                case "event" -> eventName = value;
                case "id" -> eventId = value;
                case "data" -> {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(value);
                    if (data.length() > MAXIMUM_DATA_CHARS) {
                        throw new IllegalArgumentException(
                                "Agent event data is too large.");
                    }
                }
                default -> {
                    // Retry and extension fields are intentionally ignored.
                }
            }
        }
        if (data.length() > 0) {
            throw new IllegalArgumentException(
                    "Agent event stream ended inside a frame.");
        }
    }

    private AgentEvent event(
            String eventName,
            String eventId,
            String data,
            String expectedTurn,
            int previous) {
        final JsonNode frame;
        try {
            frame = mapper.readTree(data);
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "Agent event JSON is invalid.",
                    failure);
        }
        if (frame == null || !frame.isObject()) {
            throw new IllegalArgumentException(
                    "Agent event frame is invalid.");
        }
        String turnId = text(frame, "turnId");
        int sequence = positiveInteger(frame, "sequence");
        AgentEventKind kind = AgentEventKind.fromWireValue(
                text(frame, "kind"));
        Instant createdAt = instant(frame, "createdAt");
        JsonNode payload = frame.get("payload");
        if (!expectedTurn.equals(turnId)
                || !eventId.equals(Integer.toString(sequence))
                || !eventName.equals("agent." + kind.wireValue())
                || sequence <= previous
                || sequence > MAXIMUM_SEQUENCE
                || payload == null
                || !payload.isObject()) {
            throw new IllegalArgumentException(
                    "Agent event envelope is inconsistent.");
        }
        return event(turnId, sequence, kind, createdAt, payload);
    }

    private static AgentEvent event(
            String turnId,
            int sequence,
            AgentEventKind kind,
            Instant createdAt,
            JsonNode payload) {
        Optional<String> delta = kind == AgentEventKind.TEXT_DELTA
                ? Optional.of(boundedText(
                        payload,
                        "delta",
                        MAXIMUM_DELTA_CHARS))
                : Optional.empty();
        boolean reset = kind == AgentEventKind.STARTED
                && booleanValue(
                        payload,
                        "resetPartialResponse",
                        false);
        Optional<String> failureCode = kind == AgentEventKind.FAILED
                ? Optional.of(boundedText(payload, "code", 80))
                : Optional.empty();
        boolean requiresHuman = kind == AgentEventKind.COMPLETED
                && booleanValue(payload, "requiresHuman", false);
        boolean blocked = kind == AgentEventKind.COMPLETED
                && booleanValue(payload, "blocked", false);
        return new AgentEvent(
                turnId,
                sequence,
                kind,
                createdAt,
                delta,
                reset,
                failureCode,
                requiresHuman,
                blocked);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(
                    "Agent event text field is invalid.");
        }
        return value.textValue();
    }

    private static String boundedText(
            JsonNode node,
            String field,
            int maximumLength) {
        String value = text(node, field);
        if (value.isEmpty() || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Agent event payload text is invalid.");
        }
        return value;
    }

    private static int positiveInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null
                || !value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() < 1) {
            throw new IllegalArgumentException(
                    "Agent event integer field is invalid.");
        }
        return value.intValue();
    }

    private static boolean booleanValue(
            JsonNode node,
            String field,
            boolean fallback) {
        JsonNode value = node.get(field);
        if (value == null) {
            return fallback;
        }
        if (!value.isBoolean()) {
            throw new IllegalArgumentException(
                    "Agent event boolean field is invalid.");
        }
        return value.booleanValue();
    }

    private static Instant instant(JsonNode node, String field) {
        try {
            return Instant.parse(text(node, field));
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(
                    "Agent event timestamp is invalid.",
                    failure);
        }
    }

    private static String uuid(String value) throws AgentException {
        if (value == null || !UUID_PATTERN.matcher(value).matches()) {
            throw new AgentException(
                    AgentFailureKind.INVALID_REQUEST,
                    "Agent turn ID is invalid.");
        }
        try {
            return UUID.fromString(value)
                    .toString()
                    .toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException failure) {
            throw new AgentException(
                    AgentFailureKind.INVALID_REQUEST,
                    "Agent turn ID is invalid.",
                    failure);
        }
    }

    private static Optional<String> requireCursor(
            Optional<String> value) throws AgentException {
        Optional<String> cursor = Objects.requireNonNull(
                value,
                "Agent event cursor is required.");
        if (cursor.isPresent()
                && !cursor.orElseThrow().matches("^\\d{1,9}$")) {
            throw new AgentException(
                    AgentFailureKind.INVALID_REQUEST,
                    "Agent event cursor is invalid.");
        }
        return cursor;
    }

    private static boolean streamMediaType(ResponseBody body) {
        MediaType mediaType = body.contentType();
        return mediaType != null
                && STREAM_MEDIA_TYPE.equals(
                        mediaType.type() + "/" + mediaType.subtype());
    }

    private static Optional<String> requestId(Response response) {
        String value = response.header("X-Request-Id");
        return value != null
                        && value.matches(
                                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                                        + "[1-5][0-9a-fA-F]{3}-"
                                        + "[89abAB][0-9a-fA-F]{3}-"
                                        + "[0-9a-fA-F]{12}$")
                ? Optional.of(value.toLowerCase(Locale.ROOT))
                : Optional.empty();
    }

    private static AgentFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AgentFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AgentFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AgentFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return AgentFailureKind.NOT_FOUND;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AgentFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AgentFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AgentFailureKind.SERVICE_UNAVAILABLE;
        }
        return AgentFailureKind.PROTOCOL;
    }
}
