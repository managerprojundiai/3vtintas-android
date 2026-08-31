package br.com.tresvtintas.mobile.data.appointment;

import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDraft;
import br.com.tresvtintas.mobile.core.appointment.AppointmentEdit;
import br.com.tresvtintas.mobile.core.appointment.AppointmentException;
import br.com.tresvtintas.mobile.core.appointment.AppointmentFailureKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationResult;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPage;
import br.com.tresvtintas.mobile.core.appointment.AppointmentQuery;
import br.com.tresvtintas.mobile.core.appointment.AppointmentRepository;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsiblePage;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsibleQuery;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentTransitionRequest;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentUpdateRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteAppointmentRepository
        implements AppointmentRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final AppointmentAccountScope accountScope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteAppointmentRepository(
            AppointmentAccountScope accountScope,
            MobileApi api) {
        this.accountScope = Objects.requireNonNull(
                accountScope,
                "Appointment account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public AppointmentAccountScope accountScope() {
        return accountScope;
    }

    @Override
    public AppointmentPage page(
            AppointmentQuery query,
            Optional<String> cursor) throws AppointmentException {
        requireScope(query.scope());
        try {
            return AppointmentDtoMapper.page(body(execute(api.appointments(
                    query.scope().queryValue(),
                    optionalLong(query.organizationId()),
                    query.search().orElse(null),
                    query.status()
                            .map(value -> wire(value.name()))
                            .orElse(null),
                    query.kind()
                            .map(value -> wire(value.name()))
                            .orElse(null),
                    query.from().toString(),
                    query.toExclusive().toString(),
                    optionalText(cursor),
                    query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AppointmentDetail detail(
            long appointmentId,
            AppointmentScope scope) throws AppointmentException {
        requireScope(scope);
        try {
            return AppointmentDtoMapper.detail(body(execute(
                    api.appointment(appointmentId, scope.queryValue()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AppointmentResponsiblePage responsibles(
            AppointmentResponsibleQuery query,
            Optional<String> cursor) throws AppointmentException {
        requireScope(query.scope());
        try {
            return AppointmentDtoMapper.responsibles(body(execute(
                    api.appointmentResponsibles(
                            query.scope().queryValue(),
                            optionalLong(query.organizationId()),
                            query.search().orElse(null),
                            optionalText(cursor),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AppointmentMutationResult create(
            AppointmentDraft draft,
            String idempotencyKey) throws AppointmentException {
        requireScope(draft.scope());
        try {
            Response<AppointmentMutationResponse> response = execute(
                    api.createAppointment(
                            idempotencyKey,
                            new AppointmentCreateRequest(
                                    draft.scope().queryValue(),
                                    optionalLong(draft.responsibleUserId()),
                                    optionalLong(draft.organizationId()),
                                    wire(draft.kind().name()),
                                    draft.title(),
                                    draft.description().orElse(null),
                                    draft.scheduledAt().toString(),
                                    draft.durationMinutes(),
                                    draft.location().orElse(null),
                                    optionalLong(draft.customerId()),
                                    "CREATE_APPOINTMENT")));
            return mutation(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AppointmentMutationResult update(
            long appointmentId,
            AppointmentEdit edit,
            String idempotencyKey) throws AppointmentException {
        requireScope(edit.scope());
        try {
            Response<AppointmentMutationResponse> response = execute(
                    api.updateAppointment(
                            appointmentId,
                            idempotencyKey,
                            new AppointmentUpdateRequest(
                                    edit.scope().queryValue(),
                                    edit.expectedRevision(),
                                    edit.title(),
                                    edit.description().orElse(null),
                                    edit.scheduledAt().toString(),
                                    edit.durationMinutes(),
                                    edit.location().orElse(null),
                                    optionalLong(edit.customerId()),
                                    wire(edit.kind().name()),
                                    "UPDATE_APPOINTMENT")));
            return mutation(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AppointmentMutationResult transition(
            long appointmentId,
            AppointmentScope scope,
            long expectedRevision,
            AppointmentStatus status,
            String idempotencyKey) throws AppointmentException {
        requireScope(scope);
        try {
            Response<AppointmentMutationResponse> response = execute(
                    api.transitionAppointment(
                            appointmentId,
                            idempotencyKey,
                            new AppointmentTransitionRequest(
                                    scope.queryValue(),
                                    expectedRevision,
                                    wire(status.name()),
                                    "TRANSITION_APPOINTMENT")));
            return mutation(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireScope(AppointmentScope scope)
            throws AppointmentException {
        if (!active.get()
                || scope == null
                || scope != accountScope.scope()) {
            throw new AppointmentException(
                    AppointmentFailureKind.ACCESS_REVOKED,
                    "Appointment account scope is no longer active.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws AppointmentException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new AppointmentException(
                    AppointmentFailureKind.AUTH_REJECTED,
                    "The protected appointment session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new AppointmentException(
                    isProtocolFailure(exception)
                            ? AppointmentFailureKind.PROTOCOL
                            : AppointmentFailureKind.NETWORK,
                    "The appointment response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws AppointmentException {
        if (response.body() == null) {
            throw new AppointmentException(
                    AppointmentFailureKind.PROTOCOL,
                    "The appointment response did not contain a body.");
        }
        return response.body();
    }

    private AppointmentException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AppointmentException(
                    map(details.code()),
                    "The appointment request was rejected.",
                    details.requestId(),
                    null);
        }
        return new AppointmentException(
                status(response.code()),
                "The appointment service returned an invalid error response.");
    }

    private static AppointmentFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                AppointmentFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> AppointmentFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> AppointmentFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                AppointmentFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                AppointmentFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                AppointmentFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> AppointmentFailureKind.NOT_FOUND;
            case RATE_LIMITED -> AppointmentFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> AppointmentFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                AppointmentFailureKind.SERVICE_UNAVAILABLE;
            default -> AppointmentFailureKind.PROTOCOL;
        };
    }

    private static AppointmentFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AppointmentFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AppointmentFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AppointmentFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return AppointmentFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return AppointmentFailureKind.CONFLICT;
        }
        if (code == HTTP_UNPROCESSABLE) {
            return AppointmentFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AppointmentFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AppointmentFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AppointmentFailureKind.SERVICE_UNAVAILABLE;
        }
        return AppointmentFailureKind.PROTOCOL;
    }

    private static AppointmentMutationResult mutation(
            AppointmentMutationResponse value,
            Response<?> response) throws AppointmentException {
        return new AppointmentMutationResult(
                value.appointmentId(),
                enumValue(AppointmentStatus.class, value.status()),
                value.revision(),
                value.changed(),
                replayed(response));
    }

    private static boolean replayed(Response<?> response)
            throws AppointmentException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new AppointmentException(
                    AppointmentFailureKind.PROTOCOL,
                    "The appointment idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
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

    private static Long optionalLong(OptionalLong value) {
        return value != null && value.isPresent()
                ? value.orElseThrow()
                : null;
    }

    private static String optionalText(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    private static String wire(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static AppointmentException protocol(
            IllegalArgumentException exception) {
        return new AppointmentException(
                AppointmentFailureKind.PROTOCOL,
                "The appointment response was invalid.",
                exception);
    }
}
