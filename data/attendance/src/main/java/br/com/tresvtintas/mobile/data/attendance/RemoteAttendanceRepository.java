package br.com.tresvtintas.mobile.data.attendance;

import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssigneePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessagePage;
import br.com.tresvtintas.mobile.core.attendance.AttendancePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceQuery;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReadCursorResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRepository;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AttendancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceAssigneePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceManagementRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceManagementResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReplyRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReplyResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMarkReadRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReadCursorResponse;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteAttendanceRepository
        implements AttendanceRepository {
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private static final String CONVERSATION_ID_REQUIRED =
            "Attendance conversation ID is required.";

    private final AttendanceAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteAttendanceRepository(
            AttendanceAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Attendance scope is required.");
        this.api = Objects.requireNonNull(
                api,
                "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public AttendanceAccountScope scope() {
        return scope;
    }

    @Override
    public AttendancePage page(
            AttendanceQuery query,
            Optional<String> cursor) throws AttendanceException {
        requireActive();
        Objects.requireNonNull(query, "Attendance query is required.");
        Objects.requireNonNull(cursor, "Attendance cursor is required.");
        try {
            Long organizationId = query.organizationId().isPresent()
                    ? query.organizationId().getAsLong()
                    : null;
            Call<AttendancePageDto> call = api.attendanceConversations(
                    query.search().orElse(null),
                    organizationId,
                    query.channel()
                            .map(value -> value.wireValue())
                            .orElse(null),
                    query.folder()
                            .map(value -> value.wireValue())
                            .orElse(null),
                    query.priority()
                            .map(value -> value.wireValue())
                            .orElse(null),
                    query.assignment().wireValue(),
                    cursor.orElse(null),
                    query.pageSize());
            return AttendanceDtoMapper.page(body(execute(call)));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceConversation conversation(
            String conversationId) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        try {
            Call<AttendanceConversationDto> call =
                    api.attendanceConversation(required);
            return AttendanceDtoMapper.conversation(
                    body(execute(call)));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance detail response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceMessagePage messagePage(
            String conversationId,
            Optional<String> cursor,
            int limit) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        Objects.requireNonNull(
                cursor,
                "Attendance message cursor is required.");
        if (limit < 1 || limit > 100) {
            throw new AttendanceException(
                    AttendanceFailureKind.INVALID_REQUEST,
                    "Attendance message limit is invalid.");
        }
        try {
            Call<AttendanceMessagePageDto> call = api.attendanceMessages(
                    required,
                    cursor.orElse(null),
                    limit);
            return AttendanceDtoMapper.messagePage(
                    body(execute(call)));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance message response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceReplyResult reply(
            String conversationId,
            String content,
            String idempotencyKey) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        try {
            Response<AttendanceReplyResponse> response = execute(
                    api.replyToAttendanceConversation(
                            required,
                            idempotencyKey,
                            new AttendanceReplyRequest(
                                    content,
                                    "SEND_ATTENDANCE_REPLY")));
            AttendanceReplyResponse value = body(response);
            requireReplyStatus(response.code(), value.deliveryState());
            return AttendanceDtoMapper.reply(
                    value,
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance reply response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceAssigneePage assignees(
            String conversationId,
            Optional<String> search,
            Optional<String> cursor,
            int limit) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        Objects.requireNonNull(search, "Attendance search is required.");
        Objects.requireNonNull(cursor, "Attendance cursor is required.");
        if (limit < 1 || limit > 100) {
            throw new AttendanceException(
                    AttendanceFailureKind.INVALID_REQUEST,
                    "Attendance assignee limit is invalid.");
        }
        try {
            Call<AttendanceAssigneePageDto> call =
                    api.attendanceAssignees(
                            required,
                            search.orElse(null),
                            cursor.orElse(null),
                            limit);
            return AttendanceDtoMapper.assigneePage(body(execute(call)));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance assignee response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceManagementResult manage(
            String conversationId,
            int expectedRevision,
            AttendanceManagementSelection selection,
            String idempotencyKey) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        AttendanceManagementSelection requested =
                Objects.requireNonNull(
                        selection,
                        "Attendance management selection is required.");
        Long assignedToUserId = requested.assignedToUserId().isPresent()
                ? requested.assignedToUserId().getAsLong()
                : null;
        try {
            Response<AttendanceManagementResponse> response = execute(
                    api.manageAttendanceConversation(
                            required,
                            idempotencyKey,
                            new AttendanceManagementRequest(
                                    expectedRevision,
                                    requested.folder().wireValue(),
                                    requested.priority().wireValue(),
                                    assignedToUserId,
                                    "UPDATE_ATTENDANCE_CONVERSATION")));
            if (response.code() != HTTP_OK) {
                throw new AttendanceException(
                        AttendanceFailureKind.PROTOCOL,
                        "The attendance management status is invalid.");
            }
            return AttendanceDtoMapper.management(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance management response was invalid.",
                    exception);
        }
    }

    @Override
    public AttendanceReadCursorResult markRead(
            String conversationId,
            String throughMessageId) throws AttendanceException {
        requireActive();
        String required = Objects.requireNonNull(
                conversationId,
                CONVERSATION_ID_REQUIRED);
        String prefix = required + ":";
        if (throughMessageId == null
                || !throughMessageId.startsWith(prefix)) {
            throw new AttendanceException(
                    AttendanceFailureKind.INVALID_REQUEST,
                    "Attendance read message ID is invalid.");
        }
        try {
            Response<AttendanceReadCursorResponse> response = execute(
                    api.markAttendanceConversationRead(
                            required,
                            new AttendanceMarkReadRequest(
                                    throughMessageId,
                                    "MARK_ATTENDANCE_READ")));
            if (response.code() != HTTP_OK) {
                throw new AttendanceException(
                        AttendanceFailureKind.PROTOCOL,
                        "The attendance read cursor status is invalid.");
            }
            return AttendanceDtoMapper.readCursor(body(response));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance read cursor response was invalid.",
                    exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws AttendanceException {
        if (!active.get()) {
            throw new AttendanceException(
                    AttendanceFailureKind.ACCESS_REVOKED,
                    "Attendance account scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws AttendanceException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new AttendanceException(
                    AttendanceFailureKind.AUTH_REJECTED,
                    "The protected attendance session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new AttendanceException(
                    isProtocolFailure(exception)
                            ? AttendanceFailureKind.PROTOCOL
                            : AttendanceFailureKind.NETWORK,
                    "The attendance response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws AttendanceException {
        if (response.body() == null) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance response has no body.");
        }
        return response.body();
    }

    private AttendanceException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AttendanceException(
                    map(details.code()),
                    "The attendance request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new AttendanceException(
                status(response.code()),
                "The attendance error response is invalid.");
    }

    private static AttendanceFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    AttendanceFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                    AttendanceFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> AttendanceFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    AttendanceFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    AttendanceFailureKind.IDEMPOTENCY_KEY_REUSED;
            case NOT_FOUND -> AttendanceFailureKind.NOT_FOUND;
            case INVALID_JSON, INVALID_REQUEST ->
                    AttendanceFailureKind.INVALID_REQUEST;
            case RATE_LIMITED ->
                    AttendanceFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT ->
                    AttendanceFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    AttendanceFailureKind.SERVICE_UNAVAILABLE;
            default -> AttendanceFailureKind.PROTOCOL;
        };
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
        if (code == HTTP_NOT_FOUND) {
            return AttendanceFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return AttendanceFailureKind.CONFLICT;
        }
        if (code == HTTP_UNPROCESSABLE) {
            return AttendanceFailureKind.IDEMPOTENCY_KEY_REUSED;
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

    private static boolean replayed(Response<?> response)
            throws AttendanceException {
        String value = response.headers().get(
                "X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static void requireReplyStatus(
            int status,
            String deliveryState) throws AttendanceException {
        boolean valid = status == HTTP_CREATED
                && "available".equals(deliveryState)
                || status == HTTP_ACCEPTED
                && "queued".equals(deliveryState);
        if (!valid) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "The attendance reply status is inconsistent.");
        }
    }

    private static boolean isProtocolFailure(IOException exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof JacksonException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
