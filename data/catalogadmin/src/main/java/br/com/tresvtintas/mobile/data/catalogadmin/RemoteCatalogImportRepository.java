package br.com.tresvtintas.mobile.data.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationException;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportRepository;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CatalogImportDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteCatalogImportRepository
        implements CatalogImportRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final CatalogAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCatalogImportRepository(
            CatalogAdministrationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Catalog scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public CatalogAdministrationAccountScope scope() {
        return scope;
    }

    @Override
    public Mutation upload(SourceFile source, String idempotencyKey)
            throws CatalogAdministrationException {
        requireActive();
        Objects.requireNonNull(source, "Catalog import source is required.");
        try {
            RequestBody body = RequestBody.create(
                    source.bytes(),
                    MediaType.get(source.mediaType()));
            Response<CatalogImportDtos.Mutation> response = execute(
                    api.uploadCatalogImport(
                            idempotencyKey,
                            encodedFileName(source.name()),
                            source.sha256(),
                            source.mediaType(),
                            body));
            return CatalogImportDtoMapper.mutation(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Batch get(
            String importId,
            Optional<String> cursor,
            int limit) throws CatalogAdministrationException {
        requireActive();
        try {
            return CatalogImportDtoMapper.batch(body(execute(
                    api.catalogImport(
                            importId,
                            cursor.orElse(null),
                            limit))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation confirm(
            String importId,
            int expectedRevision,
            String previewDigest,
            String idempotencyKey) throws CatalogAdministrationException {
        requireActive();
        try {
            Response<CatalogImportDtos.Mutation> response = execute(
                    api.confirmCatalogImport(
                            importId,
                            idempotencyKey,
                            new CatalogImportDtos.Confirmation(
                                    expectedRevision,
                                    previewDigest,
                                    true)));
            return CatalogImportDtoMapper.mutation(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private static String encodedFileName(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private void requireActive() throws CatalogAdministrationException {
        if (!active.get()) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.ACCESS_REVOKED,
                    "Catalog import scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws CatalogAdministrationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.AUTH_REJECTED,
                    "The protected catalog import session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new CatalogAdministrationException(
                    isProtocolFailure(exception)
                            ? CatalogAdministrationFailureKind.PROTOCOL
                            : CatalogAdministrationFailureKind.NETWORK,
                    "The catalog import response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws CatalogAdministrationException {
        if (response.body() == null) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.PROTOCOL,
                    "The catalog import response has no body.");
        }
        return response.body();
    }

    private CatalogAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new CatalogAdministrationException(
                    map(details.code()),
                    "The catalog import request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new CatalogAdministrationException(
                status(response.code()),
                "The catalog import error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws CatalogAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.PROTOCOL,
                    "The catalog import idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static CatalogAdministrationFailureKind map(
            MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    CatalogAdministrationFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                    CatalogAdministrationFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> CatalogAdministrationFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    CatalogAdministrationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    CatalogAdministrationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    CatalogAdministrationFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> CatalogAdministrationFailureKind.NOT_FOUND;
            case PAYLOAD_TOO_LARGE ->
                    CatalogAdministrationFailureKind.PAYLOAD_TOO_LARGE;
            case RATE_LIMITED -> CatalogAdministrationFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT ->
                    CatalogAdministrationFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    CatalogAdministrationFailureKind.SERVICE_UNAVAILABLE;
            default -> CatalogAdministrationFailureKind.PROTOCOL;
        };
    }

    private static CatalogAdministrationFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE) {
            return CatalogAdministrationFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return CatalogAdministrationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return CatalogAdministrationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return CatalogAdministrationFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return CatalogAdministrationFailureKind.CONFLICT;
        }
        if (code == HTTP_PAYLOAD_TOO_LARGE) {
            return CatalogAdministrationFailureKind.PAYLOAD_TOO_LARGE;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return CatalogAdministrationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return CatalogAdministrationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return CatalogAdministrationFailureKind.SERVICE_UNAVAILABLE;
        }
        return CatalogAdministrationFailureKind.PROTOCOL;
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

    private static CatalogAdministrationException protocol(
            IllegalArgumentException cause) {
        return new CatalogAdministrationException(
                CatalogAdministrationFailureKind.PROTOCOL,
                "The catalog import response was invalid.",
                cause);
    }
}
