package br.com.tresvtintas.mobile.feature.shell;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Owns only the selected presentation scope. It cannot add a store missing from the latest server
 * bootstrap and resets selection when authorization changes.
 */
public final class ShellCoordinator {
    private OptionalLong selectedOrganizationId = OptionalLong.empty();
    private Optional<ShellAccessState> current = Optional.empty();
    private List<OrganizationScope> globalOrganizations = List.of();

    public ShellAccessState apply(BootstrapSnapshot bootstrap) {
        AuthorizationSnapshot authorization = bootstrap.authorization();
        globalOrganizations = List.of();
        ShellAccessState next = switch (authorization.organizationAccessMode()) {
            case ALL -> state(bootstrap, ShellScopeKind.GLOBAL, Optional.empty());
            case NONE -> state(bootstrap, ShellScopeKind.PERSONAL, Optional.empty());
            case ASSIGNED -> assignedState(bootstrap, authorization);
        };
        current = Optional.of(next);
        return next;
    }

    public ShellAccessState selectOrganization(long organizationId) {
        ShellAccessState currentState = current.orElseThrow(() ->
                new IllegalStateException("Bootstrap must be applied before store selection."));
        AuthorizationSnapshot authorization = currentState.bootstrap().authorization();
        if (authorization.organizationPageHasMore()) {
            throw new IllegalStateException("Organization selection is not available.");
        }
        List<OrganizationScope> available = authorization.organizationAccessMode()
                == OrganizationAccessMode.ALL
                ? globalOrganizations
                : authorization.organizations();
        OrganizationScope selected = find(available, organizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization is not present in the current authorization."));
        selectedOrganizationId = OptionalLong.of(selected.id());
        ShellAccessState next = state(
                currentState.bootstrap(),
                ShellScopeKind.SELECTED_ORGANIZATION,
                Optional.of(selected));
        current = Optional.of(next);
        return next;
    }

    /**
     * Registers the server-authorized organization directory used by a global account.
     * The bootstrap intentionally does not enumerate organizations for ALL scope, so the
     * directory is fetched through the protected corporate endpoint before selection.
     */
    public void setGlobalOrganizations(List<OrganizationScope> organizations) {
        ShellAccessState currentState = current.orElseThrow(() ->
                new IllegalStateException("Bootstrap must be applied before organization directory."));
        if (currentState.bootstrap().authorization().organizationAccessMode()
                != OrganizationAccessMode.ALL) {
            throw new IllegalStateException("Global organization directory is not available.");
        }
        if (organizations == null || organizations.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Global organization directory is invalid.");
        }
        globalOrganizations = List.copyOf(organizations);
    }

    public Optional<ShellAccessState> current() {
        return current;
    }

    public void clear() {
        selectedOrganizationId = OptionalLong.empty();
        globalOrganizations = List.of();
        current = Optional.empty();
    }

    private ShellAccessState assignedState(
            BootstrapSnapshot bootstrap,
            AuthorizationSnapshot authorization) {
        if (authorization.organizationPageHasMore()) {
            selectedOrganizationId = OptionalLong.empty();
            return state(
                    bootstrap,
                    ShellScopeKind.ORGANIZATION_LIST_INCOMPLETE,
                    Optional.empty());
        }
        if (authorization.organizations().isEmpty()) {
            selectedOrganizationId = OptionalLong.empty();
            return state(
                    bootstrap,
                    ShellScopeKind.ORGANIZATION_ASSIGNMENT_REQUIRED,
                    Optional.empty());
        }
        Optional<OrganizationScope> preserved = selectedOrganizationId.isPresent()
                ? find(authorization, selectedOrganizationId.getAsLong())
                : Optional.empty();
        if (preserved.isPresent()) {
            return state(
                    bootstrap,
                    ShellScopeKind.SELECTED_ORGANIZATION,
                    preserved);
        }
        if (authorization.defaultOrganizationId().isPresent()) {
            return selectDefault(bootstrap, authorization);
        }
        selectedOrganizationId = OptionalLong.empty();
        return state(
                bootstrap,
                ShellScopeKind.REQUIRES_ORGANIZATION_SELECTION,
                Optional.empty());
    }

    private ShellAccessState selectDefault(
            BootstrapSnapshot bootstrap,
            AuthorizationSnapshot authorization) {
        long defaultId = authorization.defaultOrganizationId().orElseThrow();
        OrganizationScope selected = find(authorization, defaultId).orElseThrow();
        selectedOrganizationId = OptionalLong.of(defaultId);
        return state(
                bootstrap,
                ShellScopeKind.SELECTED_ORGANIZATION,
                Optional.of(selected));
    }

    private static Optional<OrganizationScope> find(
            List<OrganizationScope> organizations,
            long organizationId) {
        return organizations.stream()
                .filter(organization -> organization.id() == organizationId)
                .findFirst();
    }

    private static Optional<OrganizationScope> find(
            AuthorizationSnapshot authorization,
            long organizationId) {
        return find(authorization.organizations(), organizationId);
    }

    private static ShellAccessState state(
            BootstrapSnapshot bootstrap,
            ShellScopeKind kind,
            Optional<OrganizationScope> organization) {
        return new ShellAccessState(
                bootstrap,
                kind,
                organization);
    }
}
