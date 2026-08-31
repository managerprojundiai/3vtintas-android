package br.com.tresvtintas.mobile.data.finance;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceDraft;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationResult;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.core.finance.FinanceRepository;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceSettlementRequest;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Response;

public final class RemoteCorporateFinanceRepository
        implements FinanceRepository {
    private final CorporateFinanceAccountScope scope;
    private final MobileApi api;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCorporateFinanceRepository(
            CorporateFinanceAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Corporate finance scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
    }

    public CorporateFinanceAccountScope scope() {
        return scope;
    }

    @Override
    public FinancePage page(
            FinanceQuery query,
            Optional<String> cursor) throws FinanceException {
        requireActive();
        try {
            return CorporateFinanceDtoMapper.page(FinanceHttpSupport.body(
                    FinanceHttpSupport.execute(api.corporateFinanceEntries(
                            organizationId(),
                            query.search().orElse(null),
                            query.status()
                                    .map(value -> FinanceHttpSupport.wire(
                                            value.name()))
                                    .orElse(null),
                            query.type()
                                    .map(value -> FinanceHttpSupport.wire(
                                            value.name()))
                                    .orElse(null),
                            query.source()
                                    .map(value -> FinanceHttpSupport.wire(
                                            value.name()))
                                    .orElse(null),
                            FinanceHttpSupport.wire(query.due().name()),
                            query.dateBasis()
                                    == br.com.tresvtintas.mobile.core.finance
                                            .FinanceDateBasis.AGENDA
                                    ? "agenda"
                                    : null,
                            query.dueFrom()
                                    .map(Object::toString)
                                    .orElse(null),
                            query.dueToExclusive()
                                    .map(Object::toString)
                                    .orElse(null),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    @Override
    public FinanceDetail detail(long entryId) throws FinanceException {
        requireActive();
        try {
            return CorporateFinanceDtoMapper.detail(FinanceHttpSupport.body(
                    FinanceHttpSupport.execute(
                            api.corporateFinanceEntry(entryId))));
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    @Override
    public FinanceMutationResult create(
            FinanceDraft draft,
            String idempotencyKey) throws FinanceException {
        requireActive();
        if (scope.organizationId().isEmpty()) {
            throw new FinanceException(
                    FinanceFailureKind.INVALID_REQUEST,
                    "A corporate finance organization must be selected.");
        }
        try {
            Response<CorporateFinanceMutationResponse> response =
                    FinanceHttpSupport.execute(
                            api.createCorporateFinanceEntry(
                                    idempotencyKey,
                                    new CorporateFinanceCreateRequest(
                                            scope.organizationId()
                                                    .getAsLong(),
                                            FinanceHttpSupport.wire(
                                                    draft.type().name()),
                                            draft.title(),
                                            draft.amount().toPlainString(),
                                            draft.dueAt()
                                                    .map(Object::toString)
                                                    .orElse(null),
                                            draft.customerId().isPresent()
                                                    ? draft.customerId()
                                                            .orElseThrow()
                                                    : null,
                                            draft.notes().orElse(null),
                                            "CREATE_CORPORATE_FINANCIAL_ENTRY")));
            CorporateFinanceMutationResponse value =
                    FinanceHttpSupport.body(response);
            return FinanceHttpSupport.mutation(
                    FinanceAction.CREATE,
                    value.entryId(),
                    value.status(),
                    value.changed(),
                    response);
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    @Override
    public FinanceMutationResult settle(
            long entryId,
            FinancePaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) throws FinanceException {
        requireActive();
        try {
            Response<CorporateFinanceMutationResponse> response =
                    FinanceHttpSupport.execute(
                            api.settleCorporateFinanceEntry(
                                    entryId,
                                    idempotencyKey,
                                    new CorporateFinanceSettlementRequest(
                                            FinanceHttpSupport.wire(
                                                    paymentMethod.name()),
                                            FinanceHttpSupport.optionalText(
                                                    paymentReference),
                                            "SETTLE_CORPORATE_FINANCIAL_ENTRY")));
            CorporateFinanceMutationResponse value =
                    FinanceHttpSupport.body(response);
            return FinanceHttpSupport.mutation(
                    FinanceAction.SETTLE,
                    value.entryId(),
                    value.status(),
                    value.changed(),
                    response);
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    @Override
    public FinanceMutationResult cancel(
            long entryId,
            String idempotencyKey) throws FinanceException {
        requireActive();
        try {
            Response<CorporateFinanceMutationResponse> response =
                    FinanceHttpSupport.execute(
                            api.cancelCorporateFinanceEntry(
                                    entryId,
                                    idempotencyKey,
                                    new CorporateFinanceCancellationRequest(
                                            "CANCEL_CORPORATE_FINANCIAL_ENTRY")));
            CorporateFinanceMutationResponse value =
                    FinanceHttpSupport.body(response);
            return FinanceHttpSupport.mutation(
                    FinanceAction.CANCEL,
                    value.entryId(),
                    value.status(),
                    value.changed(),
                    response);
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private Long organizationId() {
        return scope.organizationId().isPresent()
                ? scope.organizationId().getAsLong()
                : null;
    }

    private void requireActive() throws FinanceException {
        if (!active.get()) {
            throw new FinanceException(
                    FinanceFailureKind.ACCESS_REVOKED,
                    "Corporate finance account scope is no longer active.");
        }
    }
}
