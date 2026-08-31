package br.com.tresvtintas.mobile.core.organizationadmin;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record OrganizationAdministrationListState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<OrganizationAdministrationException> failure,
        boolean loadingMore) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        STALE,
        ERROR,
        CLOSED
    }

    public record Snapshot(
            OrganizationAdministrationQuery query,
            List<Organization> organizations,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(query, "Organization query is required.");
            if (organizations == null
                    || organizations.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Organization snapshot is invalid.");
            }
            organizations = List.copyOf(organizations);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<Organization> organizations() {
            return List.copyOf(organizations);
        }
    }

    public OrganizationAdministrationListState {
        Objects.requireNonNull(phase, "Organization list phase is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static OrganizationAdministrationListState empty() {
        return new OrganizationAdministrationListState(
                Phase.EMPTY, Optional.empty(), Optional.empty(), false);
    }

    public static OrganizationAdministrationListState loading() {
        return new OrganizationAdministrationListState(
                Phase.LOADING, Optional.empty(), Optional.empty(), false);
    }

    public static OrganizationAdministrationListState busy(
            Snapshot snapshot,
            boolean more) {
        return new OrganizationAdministrationListState(
                Phase.LOADING, Optional.of(snapshot), Optional.empty(), more);
    }

    public static OrganizationAdministrationListState ready(Snapshot snapshot) {
        return new OrganizationAdministrationListState(
                Phase.READY, Optional.of(snapshot), Optional.empty(), false);
    }

    public static OrganizationAdministrationListState stale(
            Snapshot snapshot,
            OrganizationAdministrationException failure) {
        return new OrganizationAdministrationListState(
                Phase.STALE,
                Optional.of(snapshot),
                Optional.of(failure),
                false);
    }

    public static OrganizationAdministrationListState error(
            OrganizationAdministrationException failure) {
        return new OrganizationAdministrationListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure),
                false);
    }

    public static OrganizationAdministrationListState closed() {
        return new OrganizationAdministrationListState(
                Phase.CLOSED, Optional.empty(), Optional.empty(), false);
    }
}
