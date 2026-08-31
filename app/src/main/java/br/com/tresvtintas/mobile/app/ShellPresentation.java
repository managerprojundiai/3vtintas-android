package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellScopeKind;

record ShellPresentation(
        int title,
        int message,
        int roleLabel,
        int scopeLabel,
        String scopeValue,
        boolean chooseOrganization,
        boolean changeOrganization) {

    static ShellPresentation from(ShellAccessState state) {
        int message = switch (state.scopeKind()) {
            case GLOBAL -> R.string.shell_global_message;
            case PERSONAL -> R.string.shell_personal_message;
            case SELECTED_ORGANIZATION -> R.string.shell_store_message;
            case REQUIRES_ORGANIZATION_SELECTION -> R.string.shell_choose_store_message;
            case ORGANIZATION_ASSIGNMENT_REQUIRED -> R.string.shell_assignment_required_message;
            case ORGANIZATION_LIST_INCOMPLETE -> R.string.shell_incomplete_store_list_message;
        };
        int scopeLabel = switch (state.scopeKind()) {
            case GLOBAL -> R.string.shell_scope_global;
            case PERSONAL -> R.string.shell_scope_personal;
            case SELECTED_ORGANIZATION -> R.string.shell_scope_store;
            case REQUIRES_ORGANIZATION_SELECTION -> R.string.shell_scope_not_selected;
            case ORGANIZATION_ASSIGNMENT_REQUIRED -> R.string.shell_scope_not_assigned;
            case ORGANIZATION_LIST_INCOMPLETE -> R.string.shell_scope_incomplete;
        };
        String scopeValue = state.selectedOrganization()
                .map(organization -> organization.name())
                .orElse("");
        boolean canChoose = state.scopeKind()
                == ShellScopeKind.REQUIRES_ORGANIZATION_SELECTION;
        boolean canChange = state.bootstrap().authorization().organizations().size() > 1
                && !state.bootstrap().authorization().organizationPageHasMore();
        return new ShellPresentation(
                R.string.shell_ready_title,
                message,
                RolePresentation.label(state.bootstrap().role()),
                scopeLabel,
                scopeValue,
                canChoose,
                canChange);
    }
}
