package br.com.tresvtintas.mobile.data.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDetail;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteFailureKind;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteMutationResult;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePage;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePdfDownload;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteQuery;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteRepository;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteStatusMutationResponse;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteLaborQuoteRepository implements LaborQuoteRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private final LaborQuoteAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicReference<Call<?>> activePdfCall = new AtomicReference<>();

    public RemoteLaborQuoteRepository(LaborQuoteAccountScope scope, MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Labor quote scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public LaborQuoteAccountScope scope() {
        return scope;
    }

    @Override
    public LaborQuotePage page(LaborQuoteQuery query, Optional<String> cursor)
            throws LaborQuoteException {
        requireActive();
        return LaborQuoteDtoMapper.page(successfulBody(execute(api.laborQuotes(
                query.search().orElse(null),
                query.status()
                        .map(value -> value.name().toLowerCase(Locale.ROOT))
                        .orElse(null),
                "all".equals(query.view().queryValue())
                        ? null
                        : query.view().queryValue(),
                cursor.orElse(null),
                query.pageSize()))));
    }

    @Override
    public LaborQuoteDetail detail(long quoteId) throws LaborQuoteException {
        requireActive();
        return LaborQuoteDtoMapper.detail(successfulBody(execute(api.laborQuote(quoteId))));
    }

    @Override
    public LaborQuotePdfDownload downloadPdf(long quoteId, OutputStream destination)
            throws LaborQuoteException {
        requireActive();
        Objects.requireNonNull(destination, "Labor quote PDF destination is required.");
        Call<ResponseBody> call = api.laborQuotePdf(quoteId);
        if (!activePdfCall.compareAndSet(null, call)) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.CONFLICT,
                    "Another labor quote PDF download is active.");
        }
        try {
            return LaborQuotePdfTransport.download(
                    execute(call),
                    destination,
                    this::requireActive);
        } finally {
            activePdfCall.compareAndSet(call, null);
        }
    }

    @Override
    public LaborQuoteMutationResult create(LaborQuoteDraft draft, String key)
            throws LaborQuoteException {
        requireActive();
        return mutation(execute(api.createLaborQuote(
                key,
                LaborQuoteDtoMapper.createRequest(draft))));
    }

    @Override
    public LaborQuoteMutationResult update(
            long quoteId,
            int expectedRevision,
            LaborQuoteDraft draft,
            String key) throws LaborQuoteException {
        requireActive();
        return mutation(execute(api.updateLaborQuote(
                quoteId,
                key,
                LaborQuoteDtoMapper.updateRequest(expectedRevision, draft))));
    }

    @Override
    public LaborQuoteMutationResult duplicate(
            long quoteId,
            String idempotencyKey) throws LaborQuoteException {
        requireActive();
        return mutation(execute(api.duplicateLaborQuote(
                quoteId,
                idempotencyKey)));
    }

    @Override
    public LaborQuoteStatusMutationResult transition(
            long quoteId,
            int expectedRevision,
            LaborQuoteStatus status,
            String key) throws LaborQuoteException {
        requireActive();
        Response<LaborQuoteStatusMutationResponse> response = execute(
                api.transitionLaborQuote(
                        quoteId,
                        key,
                        LaborQuoteDtoMapper.statusRequest(expectedRevision, status)));
        LaborQuoteStatusMutationResponse body = successfulBody(response);
        return new LaborQuoteStatusMutationResult(
                body.quoteId(),
                LaborQuoteDtoMapper.status(body.previousStatus()),
                LaborQuoteDtoMapper.status(body.status()),
                body.revision(),
                new BigDecimal(body.total()),
                body.changed(),
                replayed(response));
    }

    public void close() {
        active.set(false);
        Call<?> call = activePdfCall.getAndSet(null);
        if (call != null) {
            call.cancel();
        }
    }

    private void requireActive() throws LaborQuoteException {
        if (!active.get()) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.ACCESS_REVOKED,
                    "Labor quote account scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call) throws LaborQuoteException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.AUTH_REJECTED,
                    "Protected labor quote session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new LaborQuoteException(
                    isProtocolFailure(exception)
                            ? LaborQuoteFailureKind.PROTOCOL
                            : LaborQuoteFailureKind.NETWORK,
                    "Labor quote response could not be read.",
                    exception);
        }
    }

    private static <T> T successfulBody(Response<T> response) throws LaborQuoteException {
        T body = response.body();
        if (body == null) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.PROTOCOL,
                    "Labor quote response body is absent.");
        }
        return body;
    }

    private LaborQuoteMutationResult mutation(Response<LaborQuoteMutationResponse> response)
            throws LaborQuoteException {
        LaborQuoteMutationResponse body = successfulBody(response);
        return new LaborQuoteMutationResult(
                body.quoteId(),
                body.revision(),
                new BigDecimal(body.total()),
                body.changed(),
                replayed(response));
    }

    private static boolean replayed(Response<?> response) throws LaborQuoteException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.PROTOCOL,
                    "Labor quote replay metadata is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private LaborQuoteException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new LaborQuoteException(
                    map(details.code()),
                    "Labor quote request was rejected.",
                    details.requestId(),
                    null);
        }
        return new LaborQuoteException(
                status(response.code()),
                "Labor quote service returned an invalid error response.");
    }

    private static LaborQuoteFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> LaborQuoteFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> LaborQuoteFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> LaborQuoteFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS -> LaborQuoteFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED -> LaborQuoteFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST -> LaborQuoteFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> LaborQuoteFailureKind.NOT_FOUND;
            case RATE_LIMITED -> LaborQuoteFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> LaborQuoteFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE -> LaborQuoteFailureKind.SERVICE_UNAVAILABLE;
            default -> LaborQuoteFailureKind.PROTOCOL;
        };
    }

    private static LaborQuoteFailureKind status(int value) {
        if (value == HTTP_BAD_REQUEST) {
            return LaborQuoteFailureKind.INVALID_REQUEST;
        }
        if (value == HTTP_UNAUTHORIZED) {
            return LaborQuoteFailureKind.AUTH_REJECTED;
        }
        if (value == HTTP_FORBIDDEN) {
            return LaborQuoteFailureKind.FORBIDDEN;
        }
        if (value == HTTP_NOT_FOUND) {
            return LaborQuoteFailureKind.NOT_FOUND;
        }
        if (value == HTTP_CONFLICT) {
            return LaborQuoteFailureKind.CONFLICT;
        }
        if (value == HTTP_UNPROCESSABLE) {
            return LaborQuoteFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (value == HTTP_UPGRADE_REQUIRED) {
            return LaborQuoteFailureKind.UPDATE_REQUIRED;
        }
        if (value == HTTP_TOO_MANY_REQUESTS) {
            return LaborQuoteFailureKind.RATE_LIMITED;
        }
        if (value >= HTTP_SERVER_ERROR_MINIMUM) {
            return LaborQuoteFailureKind.SERVICE_UNAVAILABLE;
        }
        return LaborQuoteFailureKind.PROTOCOL;
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
