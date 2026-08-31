package br.com.tresvtintas.mobile.data.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationException;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.CategoryMutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.KnowledgeDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.ProductDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationQuery;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationRepository;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CatalogAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteCatalogAdministrationRepository
        implements CatalogAdministrationRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final CatalogAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCatalogAdministrationRepository(
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
    public Page products(
            CatalogAdministrationQuery query,
            Optional<String> cursor) throws CatalogAdministrationException {
        requireActive();
        Objects.requireNonNull(query, "Catalog query is required.");
        try {
            return CatalogAdministrationDtoMapper.page(body(execute(
                    api.catalogAdministrationProducts(
                            query.search().orElse(null),
                            query.categoryId().isPresent()
                                    ? query.categoryId().getAsLong()
                                    : null,
                            query.active().orElse(null),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Product product(long productId)
            throws CatalogAdministrationException {
        requireActive();
        try {
            return CatalogAdministrationDtoMapper.product(body(execute(
                    api.catalogAdministrationProduct(productId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public List<Category> categories()
            throws CatalogAdministrationException {
        requireActive();
        try {
            return body(execute(api.catalogAdministrationCategories()))
                    .items()
                    .stream()
                    .map(CatalogAdministrationDtoMapper::category)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation create(ProductDraft draft, String key)
            throws CatalogAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.createCatalogAdministrationProduct(
                    key,
                    new CatalogAdministrationDtos.CreateRequest(
                            write(draft),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation update(
            long productId,
            int revision,
            ProductDraft draft,
            String key) throws CatalogAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updateCatalogAdministrationProduct(
                    productId,
                    key,
                    new CatalogAdministrationDtos.UpdateRequest(
                            "update",
                            revision,
                            write(draft),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation setActive(
            long productId,
            int revision,
            boolean activeValue,
            String key) throws CatalogAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updateCatalogAdministrationStatus(
                    productId,
                    key,
                    new CatalogAdministrationDtos.StatusRequest(
                            "status",
                            revision,
                            activeValue,
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation updateKnowledge(
            long productId,
            int revision,
            KnowledgeDraft draft,
            String key) throws CatalogAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updateCatalogAdministrationKnowledge(
                    productId,
                    key,
                    new CatalogAdministrationDtos.KnowledgeRequest(
                            "knowledge",
                            revision,
                            knowledge(draft),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public CategoryMutation createCategory(
            String name,
            Optional<String> description,
            String key) throws CatalogAdministrationException {
        requireActive();
        try {
            Response<CatalogAdministrationDtos.CategoryMutation> response =
                    execute(api.createCatalogAdministrationCategory(
                            key,
                            new CatalogAdministrationDtos.CategoryCreateRequest(
                                    name,
                                    description.orElse(null),
                                    true)));
            return CatalogAdministrationDtoMapper.categoryMutation(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private Mutation mutation(
            Response<CatalogAdministrationDtos.Mutation> response)
            throws CatalogAdministrationException {
        return CatalogAdministrationDtoMapper.mutation(
                body(response),
                replayed(response));
    }

    private static CatalogAdministrationDtos.ProductWrite write(
            ProductDraft draft) {
        Objects.requireNonNull(draft, "Product draft is required.");
        return new CatalogAdministrationDtos.ProductWrite(
                draft.categoryId().isPresent()
                        ? draft.categoryId().getAsLong()
                        : null,
                draft.name(),
                draft.description().orElse(null),
                draft.imageUrl().orElse(null),
                draft.imagePageUrl().orElse(null),
                draft.sku().orElse(null),
                draft.unit().orElse(null),
                draft.volume().orElse(null),
                draft.price(),
                draft.stock(),
                draft.brand().orElse(null));
    }

    private static CatalogAdministrationDtos.KnowledgeWrite knowledge(
            KnowledgeDraft draft) {
        Objects.requireNonNull(draft, "Knowledge draft is required.");
        return new CatalogAdministrationDtos.KnowledgeWrite(
                draft.sourceDescription().orElse(null),
                draft.synonyms().orElse(null),
                draft.application().orElse(null),
                draft.modeOfUse().orElse(null),
                draft.dilution().orElse(null),
                draft.yield().orElse(null),
                draft.intents().orElse(null),
                draft.salesArgument().orElse(null),
                draft.questions().orElse(null),
                draft.avoidSelling().orElse(null),
                draft.whatsappReply().orElse(null));
    }

    private void requireActive() throws CatalogAdministrationException {
        if (!active.get()) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.ACCESS_REVOKED,
                    "Catalog administration scope is inactive.");
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
                    "The protected catalog session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new CatalogAdministrationException(
                    isProtocolFailure(exception)
                            ? CatalogAdministrationFailureKind.PROTOCOL
                            : CatalogAdministrationFailureKind.NETWORK,
                    "The catalog administration response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws CatalogAdministrationException {
        if (response.body() == null) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.PROTOCOL,
                    "The catalog administration response has no body.");
        }
        return response.body();
    }

    private CatalogAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new CatalogAdministrationException(
                    map(details.code()),
                    "The catalog administration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new CatalogAdministrationException(
                status(response.code()),
                "The catalog administration error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws CatalogAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new CatalogAdministrationException(
                    CatalogAdministrationFailureKind.PROTOCOL,
                    "The catalog idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static CatalogAdministrationFailureKind map(MobileProblemCode code) {
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

    private CatalogAdministrationException protocol(
            IllegalArgumentException cause) {
        return new CatalogAdministrationException(
                CatalogAdministrationFailureKind.PROTOCOL,
                "The catalog administration response was invalid.",
                cause);
    }
}
