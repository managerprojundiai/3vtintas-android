package br.com.tresvtintas.mobile.data.quote;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteStatusMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.PricingContextDto;
import br.com.tresvtintas.mobile.core.network.dto.TintSalesDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDetail;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraft;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteFailureKind;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteMutationResult;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePdfDownload;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePriceListOption;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePreview;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingContext;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteQuery;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteRepository;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintColor;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintColorPage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintConfiguration;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSelection;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteMaterialQuoteRepository
        implements MaterialQuoteRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private final MaterialQuoteAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicReference<Call<?>> activePdfCall =
            new AtomicReference<>();

    public RemoteMaterialQuoteRepository(
            MaterialQuoteAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Quote scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public MaterialQuoteAccountScope scope() {
        return scope;
    }

    @Override
    public MaterialQuotePricingContext pricingContext(long organizationId)
            throws MaterialQuoteException {
        requireActive();
        PricingContextDto value = successfulBody(execute(
                api.pricingContext(organizationId)));
        return new MaterialQuotePricingContext(
                value.organizationId(),
                value.enabled(),
                value.engineMode(),
                value.policyRevision(),
                value.selectionRequired(),
                value.selections().stream()
                        .map(item -> new MaterialQuotePriceListOption(
                                item.priceListVersionPublicId(),
                                item.priceListCode(),
                                item.priceListName(),
                                item.priceListVersionNumber(),
                                item.isPrimary()))
                        .toList());
    }

    @Override
    public List<MaterialQuoteTintConfiguration> tintConfigurations(
            long organizationId,
            MaterialQuotePricingSelection pricing) throws MaterialQuoteException {
        requireActive();
        TintSalesDtos.ConfigurationResponse value = successfulBody(execute(
                api.tintSalesConfigurations(
                        organizationId,
                        pricing.selectedPriceListVersionPublicId().orElse(null),
                        pricing.expectedPolicyRevision())));
        validateTintPricing(value.pricing(), organizationId, pricing);
        return value.items().stream()
                .map(RemoteMaterialQuoteRepository::configuration)
                .toList();
    }

    @Override
    public MaterialQuoteTintColorPage tintColors(
            long organizationId,
            MaterialQuotePricingSelection pricing,
            MaterialQuoteTintConfiguration configuration,
            String search,
            int limit) throws MaterialQuoteException {
        requireActive();
        if (search == null || search.trim().length() < 2
                || search.trim().length() > 80 || limit < 1 || limit > 30) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.INVALID_REQUEST,
                    "Tint color search is invalid.");
        }
        TintSalesDtos.ColorResponse value = successfulBody(execute(
                api.tintSalesColors(
                        organizationId,
                        pricing.selectedPriceListVersionPublicId().orElse(null),
                        pricing.expectedPolicyRevision(),
                        configuration.sourceSystem(),
                        configuration.lineName(),
                        configuration.finishName(),
                        configuration.packageName(),
                        search.trim(),
                        limit)));
        validateTintPricing(value.pricing(), organizationId, pricing);
        return new MaterialQuoteTintColorPage(
                value.items().stream()
                        .map(RemoteMaterialQuoteRepository::color)
                        .toList(),
                value.hasMore());
    }

    @Override
    public MaterialQuotePage page(
            MaterialQuoteQuery query,
            Optional<String> cursor) throws MaterialQuoteException {
        requireActive();
        return MaterialQuoteDtoMapper.page(successfulBody(execute(
                api.materialQuotes(
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
    public MaterialQuoteDetail detail(long quoteId)
            throws MaterialQuoteException {
        requireActive();
        return MaterialQuoteDtoMapper.detail(
                successfulBody(execute(api.materialQuote(quoteId))));
    }

    @Override
    public MaterialQuotePdfDownload downloadPdf(
            long quoteId,
            OutputStream destination) throws MaterialQuoteException {
        requireActive();
        Objects.requireNonNull(destination, "PDF destination is required.");
        Call<ResponseBody> call = api.materialQuotePdf(quoteId);
        if (!activePdfCall.compareAndSet(null, call)) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.CONFLICT,
                    "Another quote PDF download is already active.");
        }
        try {
            Response<ResponseBody> response = execute(call);
            return MaterialQuotePdfTransport.download(
                    response,
                    destination,
                    this::requireActive);
        } finally {
            activePdfCall.compareAndSet(call, null);
        }
    }

    @Override
    public MaterialQuotePreview previewCreate(MaterialQuoteDraft draft)
            throws MaterialQuoteException {
        requireActive();
        return MaterialQuoteDtoMapper.preview(successfulBody(execute(
                api.previewMaterialQuoteCreate(
                        MaterialQuoteDtoMapper.createPreviewRequest(draft)))));
    }

    @Override
    public MaterialQuotePreview previewUpdate(
            long quoteId,
            int expectedRevision,
            MaterialQuoteDraft draft) throws MaterialQuoteException {
        requireActive();
        return MaterialQuoteDtoMapper.preview(successfulBody(execute(
                api.previewMaterialQuoteUpdate(
                        quoteId,
                        MaterialQuoteDtoMapper.updatePreviewRequest(
                                expectedRevision,
                                draft)))));
    }

    @Override
    public MaterialQuoteMutationResult create(
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint,
            String idempotencyKey) throws MaterialQuoteException {
        requireActive();
        return mutation(execute(api.createMaterialQuote(
                idempotencyKey,
                MaterialQuoteDtoMapper.createRequest(
                        draft,
                        expectedPreviewFingerprint))));
    }

    @Override
    public MaterialQuoteMutationResult update(
            long quoteId,
            int expectedRevision,
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint,
            String idempotencyKey) throws MaterialQuoteException {
        requireActive();
        return mutation(execute(api.updateMaterialQuote(
                quoteId,
                idempotencyKey,
                MaterialQuoteDtoMapper.updateRequest(
                        expectedRevision,
                        draft,
                        expectedPreviewFingerprint))));
    }

    @Override
    public MaterialQuoteMutationResult duplicate(
            long quoteId,
            String idempotencyKey) throws MaterialQuoteException {
        requireActive();
        return mutation(execute(api.duplicateMaterialQuote(
                quoteId,
                idempotencyKey)));
    }

    @Override
    public MaterialQuoteStatusMutationResult transition(
            long quoteId,
            int expectedRevision,
            MaterialQuoteStatus status,
            String idempotencyKey) throws MaterialQuoteException {
        requireActive();
        Response<MaterialQuoteStatusMutationResponse> response = execute(
                api.transitionMaterialQuote(
                        quoteId,
                        idempotencyKey,
                        MaterialQuoteDtoMapper.statusRequest(
                                expectedRevision,
                                status)));
        MaterialQuoteStatusMutationResponse body = successfulBody(response);
        return new MaterialQuoteStatusMutationResult(
                body.quoteId(),
                MaterialQuoteDtoMapper.status(body.previousStatus()),
                MaterialQuoteDtoMapper.status(body.status()),
                body.revision(),
                new BigDecimal(body.total()),
                body.pricingChanged(),
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

    private void requireActive() throws MaterialQuoteException {
        if (!active.get()) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.ACCESS_REVOKED,
                    "Quote account scope is no longer active.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws MaterialQuoteException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.AUTH_REJECTED,
                    "The protected quote session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new MaterialQuoteException(
                    isProtocolFailure(exception)
                            ? MaterialQuoteFailureKind.PROTOCOL
                            : MaterialQuoteFailureKind.NETWORK,
                    "The quote response could not be read.",
                    exception);
        }
    }

    private static <T> T successfulBody(Response<T> response)
            throws MaterialQuoteException {
        T body = response.body();
        if (body == null) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.PROTOCOL,
                    "The quote response did not contain a body.");
        }
        return body;
    }

    private MaterialQuoteMutationResult mutation(
            Response<MaterialQuoteMutationResponse> response)
            throws MaterialQuoteException {
        MaterialQuoteMutationResponse body = successfulBody(response);
        return new MaterialQuoteMutationResult(
                body.quoteId(),
                body.revision(),
                new BigDecimal(body.total()),
                body.changed(),
                replayed(response));
    }

    private static boolean replayed(Response<?> response)
            throws MaterialQuoteException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.PROTOCOL,
                    "The quote idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static MaterialQuoteTintConfiguration configuration(
            TintSalesDtos.Configuration value) {
        return new MaterialQuoteTintConfiguration(
                value.sourceSystem(),
                value.lineName(),
                value.finishName(),
                value.packageName());
    }

    private static MaterialQuoteTintColor color(TintSalesDtos.Color value) {
        return new MaterialQuoteTintColor(
                value.productId(),
                value.productName(),
                Optional.ofNullable(value.productSku()),
                Optional.ofNullable(value.productUnit()),
                Optional.ofNullable(value.productBrand()),
                value.colorId(),
                value.colorPublicId(),
                value.colorName(),
                Optional.ofNullable(value.hexColor()),
                value.tintContextId(),
                value.tintContextPublicId(),
                new MaterialQuoteTintConfiguration(
                        value.sourceSystem(),
                        value.lineName(),
                        value.finishName(),
                        value.packageName()),
                new BigDecimal(value.amount()));
    }

    private static void validateTintPricing(
            TintSalesDtos.Pricing value,
            long organizationId,
            MaterialQuotePricingSelection expected) throws MaterialQuoteException {
        boolean versionMatches = expected.selectedPriceListVersionPublicId()
                .map(value.priceListVersionPublicId()::equals)
                .orElse(true);
        if (value.organizationId() != organizationId
                || value.policyRevision() != expected.expectedPolicyRevision()
                || !versionMatches) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.PRICE_POLICY_CHANGED,
                    "Tint pricing metadata changed during selection.");
        }
    }

    private MaterialQuoteException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new MaterialQuoteException(
                    map(details.code()),
                    "The quote request was rejected.",
                    details.requestId(),
                    null);
        }
        return new MaterialQuoteException(
                status(response.code()),
                "The quote service returned an invalid error response.");
    }

    private static MaterialQuoteFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    MaterialQuoteFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> MaterialQuoteFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> MaterialQuoteFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    MaterialQuoteFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    MaterialQuoteFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    MaterialQuoteFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> MaterialQuoteFailureKind.NOT_FOUND;
            case PRICE_LIST_NOT_PUBLISHED ->
                    MaterialQuoteFailureKind.PRICE_LIST_NOT_PUBLISHED;
            case PRICE_LIST_SELECTION_REQUIRED ->
                    MaterialQuoteFailureKind.PRICE_LIST_SELECTION_REQUIRED;
            case PRICE_NOT_AVAILABLE ->
                    MaterialQuoteFailureKind.PRICE_NOT_AVAILABLE;
            case PRICE_POLICY_CHANGED, REVISION_CONFLICT ->
                    MaterialQuoteFailureKind.PRICE_POLICY_CHANGED;
            case PRICING_ENGINE_DISABLED ->
                    MaterialQuoteFailureKind.PRICING_ENGINE_DISABLED;
            case RATE_LIMITED -> MaterialQuoteFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> MaterialQuoteFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    MaterialQuoteFailureKind.SERVICE_UNAVAILABLE;
            case TINT_MAPPING_PENDING ->
                    MaterialQuoteFailureKind.TINT_MAPPING_PENDING;
            default -> MaterialQuoteFailureKind.PROTOCOL;
        };
    }

    private static MaterialQuoteFailureKind status(int value) {
        if (value == HTTP_BAD_REQUEST) {
            return MaterialQuoteFailureKind.INVALID_REQUEST;
        }
        if (value == HTTP_UNAUTHORIZED) {
            return MaterialQuoteFailureKind.AUTH_REJECTED;
        }
        if (value == HTTP_FORBIDDEN) {
            return MaterialQuoteFailureKind.FORBIDDEN;
        }
        if (value == HTTP_NOT_FOUND) {
            return MaterialQuoteFailureKind.NOT_FOUND;
        }
        if (value == HTTP_CONFLICT) {
            return MaterialQuoteFailureKind.CONFLICT;
        }
        if (value == HTTP_UNPROCESSABLE) {
            return MaterialQuoteFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (value == HTTP_UPGRADE_REQUIRED) {
            return MaterialQuoteFailureKind.UPDATE_REQUIRED;
        }
        if (value == HTTP_TOO_MANY_REQUESTS) {
            return MaterialQuoteFailureKind.RATE_LIMITED;
        }
        if (value >= HTTP_SERVER_ERROR_MINIMUM) {
            return MaterialQuoteFailureKind.SERVICE_UNAVAILABLE;
        }
        return MaterialQuoteFailureKind.PROTOCOL;
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
