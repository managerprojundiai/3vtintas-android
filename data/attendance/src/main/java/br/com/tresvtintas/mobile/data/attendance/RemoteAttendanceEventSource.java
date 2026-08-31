package br.com.tresvtintas.mobile.data.attendance;

import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeEvent;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeSource;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeSubscription;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Strict foreground SSE reader. Frames are bounded and contain only an opaque cursor, kind and
 * timestamp. No response body is logged or persisted.
 */
public final class RemoteAttendanceEventSource
        implements AttendanceRealtimeSource, AutoCloseable {
    private static final String STREAM_PATH = "attendance/events";
    private static final String STREAM_MEDIA_TYPE = "text/event-stream";
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private static final long MAXIMUM_LINE_BYTES = 4_096;
    private static final int MAXIMUM_DATA_CHARS = 4_096;
    private final AttendanceAccountScope scope;
    private final MobileNetworkClient client;
    private final ObjectMapper mapper;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Set<RemoteSubscription> subscriptions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RemoteAttendanceEventSource(
            AttendanceAccountScope scope,
            MobileNetworkClient client) {
        this.scope = Objects.requireNonNull(
                scope,
                "Attendance stream scope is required.");
        this.client = Objects.requireNonNull(
                client,
                "Attendance stream client is required.");
        mapper = MobileApiFactory.objectMapper();
    }

    public AttendanceAccountScope scope() {
        return scope;
    }

    @Override
    public AttendanceRealtimeSubscription subscribe(
            Optional<String> lastEventId,
            Listener listener) throws AttendanceException {
        if (!active.get()) {
            throw new AttendanceException(
                    AttendanceFailureKind.ACCESS_REVOKED,
                    "Attendance stream scope is inactive.");
        }
        try {
            Call call = client.newAuthenticatedEventStreamCall(
                    STREAM_PATH,
                    Objects.requireNonNull(
                            lastEventId,
                            "Last event ID is required."));
            RemoteSubscription subscription = new RemoteSubscription(
                    call,
                    Objects.requireNonNull(
                            listener,
                            "Attendance stream listener is required."));
            subscriptions.add(subscription);
            call.enqueue(subscription);
            return subscription;
        } catch (IllegalArgumentException failure) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "Attendance stream request is invalid.",
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
            implements AttendanceRealtimeSubscription, Callback {
        private final Call call;
        private final Listener listener;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RemoteSubscription(Call call, Listener listener) {
            this.call = call;
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
            listener.onFailure(new AttendanceException(
                    failure instanceof AuthenticationRequiredException
                            ? AttendanceFailureKind.AUTH_REJECTED
                            : AttendanceFailureKind.NETWORK,
                    "Attendance stream could not be opened.",
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
                    fail(AttendanceFailureKind.PROTOCOL, response);
                    return;
                }
                read(response.body().source(), listener);
                if (finish()) {
                    listener.onClosed();
                }
            } catch (IOException failure) {
                if (finish()) {
                    listener.onFailure(new AttendanceException(
                            AttendanceFailureKind.NETWORK,
                            "Attendance stream was interrupted.",
                            failure));
                }
            } catch (IllegalArgumentException failure) {
                if (finish()) {
                    listener.onFailure(new AttendanceException(
                            AttendanceFailureKind.PROTOCOL,
                            "Attendance stream frame is invalid.",
                            failure));
                }
            }
        }

        private void fail(
                AttendanceFailureKind kind,
                Response response) {
            if (!finish()) {
                return;
            }
            listener.onFailure(new AttendanceException(
                    kind,
                    "Attendance stream request was rejected.",
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
            Listener listener) throws IOException {
        String eventName = "";
        String eventId = "";
        StringBuilder data = new StringBuilder();
        long previous = -1;
        boolean first = true;
        while (!source.exhausted()) {
            String line = source.readUtf8LineStrict(MAXIMUM_LINE_BYTES);
            if (line.isEmpty()) {
                if (data.length() > 0) {
                    AttendanceRealtimeEvent event = event(
                            eventName,
                            eventId,
                            data.toString(),
                            previous,
                            first);
                    listener.onEvent(event);
                    previous = event.cursorValue();
                    first = false;
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
                                "Attendance stream data is too large.");
                    }
                }
                default -> {
                    // Retry and unknown extension fields are intentionally ignored.
                }
            }
        }
        if (data.length() > 0) {
            throw new IllegalArgumentException(
                    "Attendance stream ended inside a frame.");
        }
    }

    private AttendanceRealtimeEvent event(
            String eventName,
            String eventId,
            String data,
            long previous,
            boolean first) {
        final RealtimeFrame frame;
        try {
            frame = mapper.readValue(data, RealtimeFrame.class);
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "Attendance stream JSON is invalid.",
                    failure);
        }
        AttendanceRealtimeEvent.Kind kind = switch (frame.kind()) {
            case "ready" -> AttendanceRealtimeEvent.Kind.READY;
            case "change" -> AttendanceRealtimeEvent.Kind.CHANGE;
            case "checkpoint" -> AttendanceRealtimeEvent.Kind.CHECKPOINT;
            default -> throw new IllegalArgumentException(
                    "Attendance stream kind is invalid.");
        };
        if (!eventId.equals(frame.cursor())
                || !eventName.equals("attendance." + frame.kind())) {
            throw new IllegalArgumentException(
                    "Attendance stream envelope is inconsistent.");
        }
        AttendanceRealtimeEvent event;
        try {
            event = new AttendanceRealtimeEvent(
                    kind,
                    frame.cursor(),
                    Instant.parse(frame.occurredAt()));
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(
                    "Attendance stream timestamp is invalid.",
                    failure);
        }
        if (first && kind != AttendanceRealtimeEvent.Kind.READY) {
            throw new IllegalArgumentException(
                    "Attendance stream readiness is missing.");
        }
        if (!first
                && (kind == AttendanceRealtimeEvent.Kind.READY
                        || event.cursorValue() <= previous)) {
            throw new IllegalArgumentException(
                    "Attendance stream cursor is not monotonic.");
        }
        return event;
    }

    private static boolean streamMediaType(ResponseBody body) {
        MediaType mediaType = body.contentType();
        return mediaType != null
                && STREAM_MEDIA_TYPE.equals(mediaType.type()
                        + "/"
                        + mediaType.subtype());
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

    private static AttendanceFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AttendanceFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AttendanceFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AttendanceFailureKind.FORBIDDEN;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AttendanceFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AttendanceFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AttendanceFailureKind.SERVICE_UNAVAILABLE;
        }
        return AttendanceFailureKind.PROTOCOL;
    }

    private record RealtimeFrame(
            String kind,
            String cursor,
            String occurredAt) {
        private RealtimeFrame {
            Objects.requireNonNull(kind, "Realtime kind is required.");
            Objects.requireNonNull(cursor, "Realtime cursor is required.");
            Objects.requireNonNull(
                    occurredAt,
                    "Realtime timestamp is required.");
        }
    }
}
