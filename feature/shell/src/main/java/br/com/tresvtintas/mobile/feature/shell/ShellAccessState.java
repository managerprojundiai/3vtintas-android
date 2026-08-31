package br.com.tresvtintas.mobile.feature.shell;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import java.util.Objects;
import java.util.Optional;

public record ShellAccessState(
        BootstrapSnapshot bootstrap,
        ShellScopeKind scopeKind,
        Optional<OrganizationScope> selectedOrganization) {

    public ShellAccessState {
        Objects.requireNonNull(bootstrap, "Bootstrap snapshot is required.");
        Objects.requireNonNull(scopeKind, "Shell scope kind is required.");
        selectedOrganization = selectedOrganization == null
                ? Optional.empty()
                : selectedOrganization;
        if (scopeKind == ShellScopeKind.SELECTED_ORGANIZATION
                && selectedOrganization.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selected organization scope requires an organization.");
        }
        if (scopeKind != ShellScopeKind.SELECTED_ORGANIZATION
                && selectedOrganization.isPresent()) {
            throw new IllegalArgumentException(
                    "Only selected organization scope may retain an organization.");
        }
    }

    public boolean isOperational() {
        return switch (scopeKind) {
            case GLOBAL, PERSONAL, SELECTED_ORGANIZATION -> true;
            default -> false;
        };
    }
}
