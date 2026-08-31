package br.com.tresvtintas.mobile.data.whatsappadmin;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.WhatsAppAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationException;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationRepository;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteWhatsAppAdministrationRepository
        implements WhatsAppAdministrationRepository {
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final WhatsAppAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteWhatsAppAdministrationRepository(
            WhatsAppAdministrationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public WhatsAppAdministrationAccountScope scope() {
        return scope;
    }

    @Override
    public Snapshot load() throws WhatsAppAdministrationException {
        requireActive();
        try {
            return WhatsAppAdministrationDtoMapper.snapshot(body(execute(
                    api.whatsAppAdministrationStores())));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public ActionResult configureMeta(
            long organizationId,
            String phoneNumberId,
            Optional<String> phoneNumber,
            String idempotencyKey) throws WhatsAppAdministrationException {
        return mutation(api.configureMetaWhatsApp(
                organizationId,
                idempotencyKey,
                WhatsAppAdministrationDtoMapper.configureMetaRequest(
                        phoneNumberId,
                        phoneNumber)));
    }

    @Override
    public ActionResult provisionEvolution(
            long organizationId,
            String idempotencyKey) throws WhatsAppAdministrationException {
        return mutation(api.provisionEvolutionWhatsApp(
                organizationId,
                idempotencyKey,
                WhatsAppAdministrationDtoMapper.provisionEvolutionRequest()));
    }

    @Override
    public ActionResult setMode(
            long organizationId,
            WhatsAppChannelMode mode,
            String idempotencyKey) throws WhatsAppAdministrationException {
        return mutation(api.setWhatsAppMode(
                organizationId,
                idempotencyKey,
                WhatsAppAdministrationDtoMapper.setModeRequest(mode)));
    }

    @Override
    public ActionResult refreshEvolution(
            long connectionId,
            String idempotencyKey) throws WhatsAppAdministrationException {
        return mutation(api.refreshEvolutionWhatsApp(
                connectionId,
                idempotencyKey,
                WhatsAppAdministrationDtoMapper.refreshEvolutionRequest()));
    }

    @Override
    public EphemeralQr requestQr(long connectionId)
            throws WhatsAppAdministrationException {
        requireActive();
        try {
            return WhatsAppAdministrationDtoMapper.qr(body(execute(
                    api.requestEvolutionWhatsAppQr(
                            connectionId,
                            WhatsAppAdministrationDtoMapper.qrRequest()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private ActionResult mutation(Call<WhatsAppAdministrationDtos.ActionResult> call)
            throws WhatsAppAdministrationException {
        requireActive();
        try {
            Response<WhatsAppAdministrationDtos.ActionResult> response =
                    execute(call);
            return WhatsAppAdministrationDtoMapper.action(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    private void requireActive() throws WhatsAppAdministrationException {
        if (!active.get()) {
            throw new WhatsAppAdministrationException(
                    WhatsAppAdministrationFailureKind.ACCESS_REVOKED,
                    "WhatsApp administration scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws WhatsAppAdministrationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new WhatsAppAdministrationException(
                    WhatsAppAdministrationFailureKind.AUTH_REJECTED,
                    "The protected WhatsApp session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new WhatsAppAdministrationException(
                    isProtocolFailure(exception)
                            ? WhatsAppAdministrationFailureKind.PROTOCOL
                            : WhatsAppAdministrationFailureKind.NETWORK,
                    "The WhatsApp response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws WhatsAppAdministrationException {
        if (response.body() == null) {
            throw new WhatsAppAdministrationException(
                    WhatsAppAdministrationFailureKind.PROTOCOL,
                    "The WhatsApp response has no body.");
        }
        return response.body();
    }

    private WhatsAppAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new WhatsAppAdministrationException(
                    map(details.code()),
                    "The WhatsApp administration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new WhatsAppAdministrationException(
                status(response.code()),
                "The WhatsApp error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws WhatsAppAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new WhatsAppAdministrationException(
                    WhatsAppAdministrationFailureKind.PROTOCOL,
                    "The WhatsApp idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static WhatsAppAdministrationFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    WhatsAppAdministrationFailureKind.AUTH_REJECTED;
            case FORBIDDEN -> WhatsAppAdministrationFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST ->
                    WhatsAppAdministrationFailureKind.INVALID_REQUEST;
            case RESOURCE_CONFLICT -> WhatsAppAdministrationFailureKind.CONFLICT;
            case IDEMPOTENCY_IN_PROGRESS ->
                    WhatsAppAdministrationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    WhatsAppAdministrationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case RATE_LIMITED -> WhatsAppAdministrationFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                    WhatsAppAdministrationFailureKind.SERVICE_UNAVAILABLE;
            case APP_UPDATE_REQUIRED ->
                    WhatsAppAdministrationFailureKind.UPDATE_REQUIRED;
            default -> WhatsAppAdministrationFailureKind.PROTOCOL;
        };
    }

    private static WhatsAppAdministrationFailureKind status(int code) {
        if (code == HTTP_UNAUTHORIZED) {
            return WhatsAppAdministrationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return WhatsAppAdministrationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_CONFLICT) {
            return WhatsAppAdministrationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return WhatsAppAdministrationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return WhatsAppAdministrationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return WhatsAppAdministrationFailureKind.SERVICE_UNAVAILABLE;
        }
        return WhatsAppAdministrationFailureKind.INVALID_REQUEST;
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

    private static WhatsAppAdministrationException protocol(Throwable cause) {
        return new WhatsAppAdministrationException(
                WhatsAppAdministrationFailureKind.PROTOCOL,
                "The WhatsApp response was invalid.",
                cause);
    }
}
