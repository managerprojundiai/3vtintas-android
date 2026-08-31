package br.com.tresvtintas.mobile.core.corporatefinance;

import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import java.util.Optional;

public record CorporateFinanceOrganizationState(
        Phase phase,
        Optional<CorporateFinanceOrganizationSnapshot> snapshot,
        Optional<FinanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        LOADING_MORE,
        ERROR,
        CLOSED
    }

    public CorporateFinanceOrganizationState {
        if (phase == null) {
            throw new IllegalArgumentException(
                    "Corporate finance organization phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Corporate finance organization phase requires a snapshot.");
        }
    }

    public static CorporateFinanceOrganizationState empty() {
        return phase(Phase.EMPTY);
    }

    public static CorporateFinanceOrganizationState loading() {
        return phase(Phase.LOADING);
    }

    public static CorporateFinanceOrganizationState loadingMore(
            CorporateFinanceOrganizationSnapshot value) {
        return snapshot(Phase.LOADING_MORE, value);
    }

    public static CorporateFinanceOrganizationState ready(
            CorporateFinanceOrganizationSnapshot value) {
        return snapshot(Phase.READY, value);
    }

    public static CorporateFinanceOrganizationState error(
            FinanceException value) {
        return new CorporateFinanceOrganizationState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static CorporateFinanceOrganizationState closed() {
        return phase(Phase.CLOSED);
    }

    private static CorporateFinanceOrganizationState phase(Phase value) {
        return new CorporateFinanceOrganizationState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static CorporateFinanceOrganizationState snapshot(
            Phase phase,
            CorporateFinanceOrganizationSnapshot value) {
        return new CorporateFinanceOrganizationState(
                phase,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }
}
