package br.com.tresvtintas.mobile.data.commission;

import br.com.tresvtintas.mobile.core.commission.CommissionDetail;
import br.com.tresvtintas.mobile.core.commission.CommissionException;
import br.com.tresvtintas.mobile.core.commission.CommissionFailureKind;
import br.com.tresvtintas.mobile.core.commission.CommissionPage;
import br.com.tresvtintas.mobile.core.commission.CommissionMutationCommand;
import br.com.tresvtintas.mobile.core.commission.CommissionMutationResult;
import br.com.tresvtintas.mobile.core.commission.CommissionQuery;
import br.com.tresvtintas.mobile.core.commission.CommissionRepository;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CommissionApprovalRequest;
import br.com.tresvtintas.mobile.core.network.dto.CommissionCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.CommissionMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CommissionPaymentRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteCommissionRepository implements CommissionRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final CommissionAccountScope accountScope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCommissionRepository(
            CommissionAccountScope accountScope,
            MobileApi api) {
        this.accountScope = Objects.requireNonNull(
                accountScope,
                "Commission account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public CommissionAccountScope accountScope() {
        return accountScope;
    }

    @Override
    public CommissionPage page(
            CommissionQuery query,
            Optional<String> cursor) throws CommissionException {
        requireActive();
        try {
            Long organizationId = query.organizationId().isPresent()
                    ? query.organizationId().orElseThrow()
                    : null;
            return CommissionDtoMapper.page(body(execute(api.commissions(
                    query.scope().queryValue(),
                    query.search().orElse(null),
                    query.status()
                            .map(value -> wire(value.name()))
                            .orElse(null),
                    query.kind()
                            .map(value -> wire(value.name()))
                            .orElse(null),
                    organizationId,
                    query.from().map(Object::toString).orElse(null),
                    query.to().map(Object::toString).orElse(null),
                    cursor.orElse(null),
                    query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public CommissionDetail detail(
            long commissionId,
            CommissionScope scope) throws CommissionException {
        requireActive();
        try {
            return CommissionDtoMapper.detail(body(execute(
                    api.commission(commissionId, scope.queryValue()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public CommissionMutationResult transition(
            CommissionMutationCommand command,
            String idempotencyKey) throws CommissionException {
        requireActive();
        try {
            Response<CommissionMutationResponse> response = execute(
                    mutationCall(command, idempotencyKey));
            CommissionMutationResponse value = body(response);
            return new CommissionMutationResult(
                    command.action(),
                    value.commissionId(),
                    enumValue(CommissionStatus.class, value.status()),
                    value.revision(),
                    value.changed(),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws CommissionException {
        if (!active.get()) {
            throw new CommissionException(
                    CommissionFailureKind.ACCESS_REVOKED,
                    "Commission account scope is no longer active.");
        }
    }

    private <T> Response<T> execute(Call<T> call) throws CommissionException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new CommissionException(
                    CommissionFailureKind.AUTH_REJECTED,
                    "The protected commission session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new CommissionException(
                    isProtocolFailure(exception)
                            ? CommissionFailureKind.PROTOCOL
                            : CommissionFailureKind.NETWORK,
                    "The commission response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    private static <T> T body(Response<T> response) throws CommissionException {
        if (response.body() == null) {
            throw new CommissionException(
                    CommissionFailureKind.PROTOCOL,
                    "The commission response did not contain a body.");
        }
        return response.body();
    }

    private CommissionException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new CommissionException(
                    map(details.code()),
                    "The commission request was rejected.",
                    details.requestId(),
                    null);
        }
        return new CommissionException(
                status(response.code()),
                "The commission service returned an invalid error response.");
    }

    private static CommissionFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                CommissionFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> CommissionFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> CommissionFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    CommissionFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    CommissionFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                CommissionFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> CommissionFailureKind.NOT_FOUND;
            case RATE_LIMITED -> CommissionFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> CommissionFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                CommissionFailureKind.SERVICE_UNAVAILABLE;
            default -> CommissionFailureKind.PROTOCOL;
        };
    }

    private static CommissionFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return CommissionFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return CommissionFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return CommissionFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return CommissionFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return CommissionFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return CommissionFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return CommissionFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return CommissionFailureKind.SERVICE_UNAVAILABLE;
        }
        return CommissionFailureKind.PROTOCOL;
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

    private static String wire(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private Call<CommissionMutationResponse> mutationCall(
            CommissionMutationCommand command,
            String idempotencyKey) {
        return switch (command.action()) {
            case APPROVE -> api.approveCommission(
                    command.commissionId(),
                    idempotencyKey,
                    new CommissionApprovalRequest(
                            command.expectedRevision(),
                            true));
            case CANCEL -> api.cancelCommission(
                    command.commissionId(),
                    idempotencyKey,
                    new CommissionCancellationRequest(
                            command.expectedRevision(),
                            true,
                            command.cancellationReason().orElseThrow()));
            case PAY -> api.payCommission(
                    command.commissionId(),
                    idempotencyKey,
                    new CommissionPaymentRequest(
                            command.expectedRevision(),
                            true,
                            wire(command.paymentMethod().orElseThrow().name()),
                            command.paymentReference().orElse(null)));
        };
    }

    private static boolean replayed(Response<?> response)
            throws CommissionException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new CommissionException(
                    CommissionFailureKind.PROTOCOL,
                    "The commission idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static CommissionException protocol(
            IllegalArgumentException exception) {
        return new CommissionException(
                CommissionFailureKind.PROTOCOL,
                "The commission response was invalid.",
                exception);
    }
}
