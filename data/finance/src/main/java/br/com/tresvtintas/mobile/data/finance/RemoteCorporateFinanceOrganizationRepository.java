package br.com.tresvtintas.mobile.data.finance;

import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationPage;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationRepository;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteCorporateFinanceOrganizationRepository
        implements CorporateFinanceOrganizationRepository {
    private final CorporateFinanceAccountScope scope;
    private final MobileApi api;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCorporateFinanceOrganizationRepository(
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
    public CorporateFinanceOrganizationPage page(
            Optional<String> search,
            Optional<String> cursor,
            int pageSize) throws FinanceException {
        requireActive();
        if (pageSize < 1 || pageSize > 100) {
            throw new FinanceException(
                    FinanceFailureKind.INVALID_REQUEST,
                    "Corporate finance organization page size is invalid.");
        }
        try {
            return CorporateFinanceDtoMapper.organizations(
                    FinanceHttpSupport.body(FinanceHttpSupport.execute(
                            api.corporateFinanceOrganizations(
                                    FinanceHttpSupport.optionalText(search),
                                    cursor == null
                                            ? null
                                            : cursor.orElse(null),
                                    pageSize))));
        } catch (IllegalArgumentException exception) {
            throw FinanceHttpSupport.protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws FinanceException {
        if (!active.get()) {
            throw new FinanceException(
                    FinanceFailureKind.ACCESS_REVOKED,
                    "Corporate finance account scope is no longer active.");
        }
    }
}
