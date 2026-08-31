package br.com.tresvtintas.mobile.data.catalog.remote;

import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogPriceListOption;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContext;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CatalogPageDto;
import br.com.tresvtintas.mobile.core.network.dto.PricingContextDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import retrofit2.Response;

public final class RetrofitCatalogRemote implements CatalogRemote {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private static final long MINIMUM_ORGANIZATION_ID = 1L;
    private final MobileApi api;
    private final boolean canReadPrices;
    private final long organizationId;
    private final ProblemDetailsParser problemParser;
    private volatile Optional<CatalogPricingContext> currentPricing = Optional.empty();

    public RetrofitCatalogRemote(
            MobileApi api,
            boolean canReadPrices,
            long organizationId) {
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.canReadPrices = canReadPrices;
        if (organizationId < MINIMUM_ORGANIZATION_ID) {
            throw new IllegalArgumentException("Catalog organization ID must be positive.");
        }
        this.organizationId = organizationId;
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    @Override
    public Optional<CatalogPricingContext> pricingContext() throws CatalogException {
        if (!canReadPrices) {
            return Optional.empty();
        }
        Response<PricingContextDto> response;
        try {
            response = api.pricingContext(organizationId).execute();
        } catch (AuthenticationRequiredException exception) {
            throw authFailure(exception);
        } catch (IOException exception) {
            throw transportFailure(exception);
        }
        if (!response.isSuccessful()) {
            throw mapFailure(response);
        }
        PricingContextDto body = response.body();
        if (body == null || !body.enabled() || !"ACTIVE".equals(body.engineMode())) {
            throw new CatalogException(
                    CatalogFailureKind.PRICE_NOT_AVAILABLE,
                    "Catalog pricing is not active.");
        }
        try {
            Optional<String> preserved = currentPricing
                    .flatMap(CatalogPricingContext::selectedVersionPublicId)
                    .filter(selected -> body.selections().stream().anyMatch(item ->
                            item.priceListVersionPublicId().equals(selected)));
            CatalogPricingContext mapped = new CatalogPricingContext(
                    body.organizationId(),
                    body.policyRevision(),
                    body.selectionRequired(),
                    body.selections().stream()
                            .map(item -> new CatalogPriceListOption(
                                    item.priceListVersionPublicId(),
                                    item.priceListCode(),
                                    item.priceListName(),
                                    item.priceListVersionNumber(),
                                    item.isPrimary()))
                            .collect(Collectors.toList()),
                    preserved);
            if (!mapped.selectionRequired()) {
                mapped = mapped.select(mapped.primary().versionPublicId());
            }
            currentPricing = Optional.of(mapped);
            return currentPricing;
        } catch (IllegalArgumentException exception) {
            throw new CatalogException(
                    CatalogFailureKind.PROTOCOL,
                    "The pricing context violated its contract.",
                    exception);
        }
    }

    @Override
    public void selectPriceList(String versionPublicId) throws CatalogException {
        if (!canReadPrices) {
            throw new CatalogException(
                    CatalogFailureKind.FORBIDDEN,
                    "Catalog pricing is restricted.");
        }
        CatalogPricingContext context = currentPricing.isPresent()
                ? currentPricing.orElseThrow()
                : pricingContext().orElseThrow(() -> new CatalogException(
                        CatalogFailureKind.PRICE_NOT_AVAILABLE,
                        "Catalog pricing is unavailable."));
        try {
            currentPricing = Optional.of(context.select(versionPublicId));
        } catch (IllegalArgumentException exception) {
            throw new CatalogException(
                    CatalogFailureKind.INVALID_REQUEST,
                    "Selected catalog table is invalid.",
                    exception);
        }
    }

    @Override
    public boolean canReadPrices() {
        return canReadPrices;
    }

    @Override
    public CatalogPage fetch(CatalogQuery query, Optional<String> cursor)
            throws CatalogException {
        Response<CatalogPageDto> response;
        try {
            CatalogPricingContext context = canReadPrices
                    ? currentPricing.orElseThrow(() -> new CatalogException(
                            CatalogFailureKind.PRICE_SELECTION_REQUIRED,
                            "Catalog table selection is required."))
                    : null;
            String selectedVersionPublicId = context == null
                    ? null
                    : context.selectedVersionPublicId().orElseThrow(() ->
                            new CatalogException(
                                    CatalogFailureKind.PRICE_SELECTION_REQUIRED,
                                    "Catalog table selection is required."));
            response = (canReadPrices ? api.catalogProducts(
                            organizationId,
                            selectedVersionPublicId,
                            context.policyRevision(),
                            query.search().orElse(null),
                            query.categoryId().isPresent()
                                    ? query.categoryId().getAsLong()
                                    : null,
                            cursor.orElse(null),
                            query.pageSize()) : api.catalogProductInformation(
                            query.search().orElse(null),
                            query.categoryId().isPresent()
                                    ? query.categoryId().getAsLong()
                                    : null,
                            cursor.orElse(null),
                            query.pageSize()))
                    .execute();
        } catch (AuthenticationRequiredException exception) {
            throw authFailure(exception);
        } catch (IOException exception) {
            throw transportFailure(exception);
        }
        if (!response.isSuccessful()) {
            throw mapFailure(response);
        }
        CatalogPageDto body = response.body();
        if (body == null) {
            throw new CatalogException(
                    CatalogFailureKind.PROTOCOL,
                    "The catalog response did not contain a body.");
        }
        try {
            if (canReadPrices) {
                validatePricing(body);
            } else if (body.pricing() != null) {
                throw new IllegalArgumentException(
                        "Restricted catalog unexpectedly returned pricing metadata.");
            }
            return CatalogDtoMapper.toDomain(body);
        } catch (IllegalArgumentException exception) {
            throw new CatalogException(
                    CatalogFailureKind.PROTOCOL,
                    "The catalog response violated its contract.",
                    exception);
        }
    }

    private CatalogException mapFailure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new CatalogException(
                    mapProblemCode(details.code()),
                    "The catalog request was rejected.",
                    details.requestId(),
                    null);
        }
        return new CatalogException(
                mapStatus(response.code()),
                "The catalog service returned an invalid error response.");
    }

    private static CatalogFailureKind mapProblemCode(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> CatalogFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> CatalogFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> CatalogFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST -> CatalogFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> CatalogFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE -> CatalogFailureKind.SERVICE_UNAVAILABLE;
            case PRICE_LIST_SELECTION_REQUIRED -> CatalogFailureKind.PRICE_SELECTION_REQUIRED;
            case PRICE_NOT_AVAILABLE, PRICE_LIST_NOT_PUBLISHED,
                    TINT_MAPPING_PENDING, PRICING_ENGINE_DISABLED ->
                    CatalogFailureKind.PRICE_NOT_AVAILABLE;
            case PRICE_POLICY_CHANGED, REVISION_CONFLICT ->
                    CatalogFailureKind.PRICE_POLICY_CHANGED;
            default -> CatalogFailureKind.PROTOCOL;
        };
    }

    private static CatalogFailureKind mapStatus(int status) {
        if (status == HTTP_BAD_REQUEST) {
            return CatalogFailureKind.INVALID_REQUEST;
        }
        if (status == HTTP_UNAUTHORIZED) {
            return CatalogFailureKind.AUTH_REJECTED;
        }
        if (status == HTTP_FORBIDDEN) {
            return CatalogFailureKind.FORBIDDEN;
        }
        if (status == HTTP_UPGRADE_REQUIRED) {
            return CatalogFailureKind.UPDATE_REQUIRED;
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return CatalogFailureKind.RATE_LIMITED;
        }
        if (status >= HTTP_SERVER_ERROR_MINIMUM) {
            return CatalogFailureKind.SERVICE_UNAVAILABLE;
        }
        return CatalogFailureKind.PROTOCOL;
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

    private void validatePricing(CatalogPageDto body) {
        CatalogPricingContext expected = currentPricing.orElseThrow();
        if (body.pricing() == null
                || body.pricing().organizationId() != organizationId
                || body.pricing().policyRevision() != expected.policyRevision()
                || !body.pricing().priceListVersionPublicId().equals(
                        expected.selectedVersionPublicId().orElseThrow())) {
            throw new IllegalArgumentException(
                    "Catalog pricing metadata does not match the requested context.");
        }
    }

    private static CatalogException authFailure(AuthenticationRequiredException exception) {
        return new CatalogException(
                CatalogFailureKind.AUTH_REJECTED,
                "The protected catalog session is unavailable.",
                exception);
    }

    private static CatalogException transportFailure(IOException exception) {
        return new CatalogException(
                isProtocolFailure(exception)
                        ? CatalogFailureKind.PROTOCOL
                        : CatalogFailureKind.NETWORK,
                "The catalog response could not be read.",
                exception);
    }

}
