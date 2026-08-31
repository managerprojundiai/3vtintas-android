package br.com.tresvtintas.mobile.data.customer.remote;

import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerMutationResult;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CustomerDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CustomerPageDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import retrofit2.Call;
import retrofit2.Response;

public final class RetrofitCustomerRemote implements CustomerRemote {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;

    public RetrofitCustomerRemote(MobileApi api) {
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    @Override
    public CustomerPage page(
            CustomerQuery query,
            Optional<String> cursor) throws CustomerException {
        Response<CustomerPageDto> response = execute(api.customers(
                query.search().orElse(null),
                cursor.orElse(null),
                query.pageSize()));
        CustomerPageDto body = successfulBody(response);
        try {
            return CustomerDtoMapper.toDomain(body);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    @Override
    public CustomerDetail detail(long customerId) throws CustomerException {
        Response<CustomerDetailDto> response = execute(api.customer(customerId));
        CustomerDetailDto body = successfulBody(response);
        try {
            return CustomerDtoMapper.toDomain(body);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    @Override
    public CustomerMutationResult create(
            OptionalLong organizationId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException {
        Response<CustomerMutationResponse> response = execute(
                api.createCustomer(
                        idempotencyKey,
                        CustomerDtoMapper.createRequest(
                                organizationId,
                                customer)));
        return mutationResult(response);
    }

    @Override
    public CustomerMutationResult update(
            long customerId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException {
        Response<CustomerMutationResponse> response = execute(
                api.updateCustomer(
                        customerId,
                        idempotencyKey,
                        CustomerDtoMapper.updateRequest(customer)));
        return mutationResult(response);
    }

    private <T> Response<T> execute(Call<T> call) throws CustomerException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw mapFailure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new CustomerException(
                    CustomerFailureKind.AUTH_REJECTED,
                    "The protected customer session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new CustomerException(
                    isProtocolFailure(exception)
                            ? CustomerFailureKind.PROTOCOL
                            : CustomerFailureKind.NETWORK,
                    "The customer response could not be read.",
                    exception);
        }
    }

    private <T> T successfulBody(Response<T> response)
            throws CustomerException {
        T body = response.body();
        if (body == null) {
            throw new CustomerException(
                    CustomerFailureKind.PROTOCOL,
                    "The customer response did not contain a body.");
        }
        return body;
    }

    private CustomerMutationResult mutationResult(
            Response<CustomerMutationResponse> response)
            throws CustomerException {
        CustomerMutationResponse body = successfulBody(response);
        String replayed = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(replayed) && !"false".equals(replayed)) {
            throw new CustomerException(
                    CustomerFailureKind.PROTOCOL,
                    "The customer idempotency response is invalid.");
        }
        return new CustomerMutationResult(
                body.customerId(),
                body.changed(),
                Boolean.parseBoolean(replayed));
    }

    private CustomerException mapFailure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new CustomerException(
                    mapProblemCode(details.code()),
                    "The customer request was rejected.",
                    details.requestId(),
                    null);
        }
        return new CustomerException(
                mapStatus(response.code()),
                "The customer service returned an invalid error response.");
    }

    private static CustomerFailureKind mapProblemCode(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> CustomerFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> CustomerFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> CustomerFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    CustomerFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    CustomerFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    CustomerFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> CustomerFailureKind.NOT_FOUND;
            case RATE_LIMITED -> CustomerFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> CustomerFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE -> CustomerFailureKind.SERVICE_UNAVAILABLE;
            default -> CustomerFailureKind.PROTOCOL;
        };
    }

    private static CustomerFailureKind mapStatus(int status) {
        if (status == HTTP_BAD_REQUEST) {
            return CustomerFailureKind.INVALID_REQUEST;
        }
        if (status == HTTP_UNAUTHORIZED) {
            return CustomerFailureKind.AUTH_REJECTED;
        }
        if (status == HTTP_FORBIDDEN) {
            return CustomerFailureKind.FORBIDDEN;
        }
        if (status == HTTP_NOT_FOUND) {
            return CustomerFailureKind.NOT_FOUND;
        }
        if (status == HTTP_CONFLICT) {
            return CustomerFailureKind.CONFLICT;
        }
        if (status == HTTP_UNPROCESSABLE) {
            return CustomerFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (status == HTTP_UPGRADE_REQUIRED) {
            return CustomerFailureKind.UPDATE_REQUIRED;
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return CustomerFailureKind.RATE_LIMITED;
        }
        if (status >= HTTP_SERVER_ERROR_MINIMUM) {
            return CustomerFailureKind.SERVICE_UNAVAILABLE;
        }
        return CustomerFailureKind.PROTOCOL;
    }

    private static CustomerException protocolFailure(
            IllegalArgumentException exception) {
        return new CustomerException(
                CustomerFailureKind.PROTOCOL,
                "The customer response violated its contract.",
                exception);
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
