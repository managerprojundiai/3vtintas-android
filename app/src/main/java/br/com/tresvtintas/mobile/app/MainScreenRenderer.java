package br.com.tresvtintas.mobile.app;

import android.view.View;
import br.com.tresvtintas.mobile.app.databinding.ActivityMainBinding;
import br.com.tresvtintas.mobile.core.auth.AuthenticatedSession;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import java.util.Optional;

final class MainScreenRenderer {
    private final ActivityMainBinding binding;

    MainScreenRenderer(ActivityMainBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("Main screen binding is required.");
        }
        this.binding = binding;
    }

    void environment(String environment) {
        binding.environmentValue.setText(FoundationStatus.formatEnvironment(environment));
    }

    ScreenAction auth(
            AuthPresentation presentation,
            Optional<AuthenticatedSession> session) {
        binding.authRoot.setVisibility(View.VISIBLE);
        binding.shellRoot.setVisibility(View.GONE);
        headline(presentation.title(), presentation.message(), presentation.busy());
        ScreenAction action = switch (presentation.primaryAction()) {
            case SIGN_IN -> ScreenAction.SIGN_IN;
            case RETRY_RESTORE -> ScreenAction.RETRY_AUTH;
            case NONE -> ScreenAction.NONE;
        };
        primary(
                presentation.primaryAction() != AuthPresentation.Action.NONE,
                presentation.primaryLabel());
        binding.logoutAction.setVisibility(presentation.showLogout() ? View.VISIBLE : View.GONE);
        binding.accountCard.setVisibility(session.isPresent() ? View.VISIBLE : View.GONE);
        session.ifPresent(this::account);
        return action;
    }

    ScreenAction bootstrap(BootstrapPresentation presentation) {
        binding.authRoot.setVisibility(View.VISIBLE);
        binding.shellRoot.setVisibility(View.GONE);
        headline(presentation.title(), presentation.message(), presentation.busy());
        primary(presentation.retryAllowed(), R.string.bootstrap_retry_button);
        binding.logoutAction.setVisibility(View.VISIBLE);
        return presentation.retryAllowed()
                ? ScreenAction.RETRY_BOOTSTRAP
                : ScreenAction.NONE;
    }

    ScreenAction shell(ShellAccessState access) {
        ShellPresentation presentation = ShellPresentation.from(access);
        binding.authRoot.setVisibility(View.GONE);
        binding.shellRoot.setVisibility(View.VISIBLE);
        binding.shellToolbar.setSubtitle(presentation.roleLabel());
        binding.shellGreeting.setText(
                binding.getRoot().getContext().getString(
                        R.string.shell_greeting,
                        firstName(access.bootstrap().user().name())));
        binding.shellRole.setText(presentation.roleLabel());
        binding.shellScope.setText(presentation.scopeValue().isBlank()
                ? binding.getRoot().getContext().getString(presentation.scopeLabel())
                : binding.getRoot().getContext().getString(
                        R.string.shell_scope_value,
                        binding.getRoot().getContext().getString(presentation.scopeLabel()),
                        presentation.scopeValue()));
        int capabilityCount = access.bootstrap().authorization().capabilities().size();
        String revision = access.bootstrap().authorization().revision().substring(0, 8);
        binding.shellCapabilities.setText(
                binding.getRoot().getContext().getResources().getQuantityString(
                        R.plurals.shell_capability_summary,
                        capabilityCount,
                        capabilityCount,
                        revision));
        boolean organizationAction = presentation.chooseOrganization()
                || presentation.changeOrganization();
        binding.changeStoreAction.setVisibility(
                organizationAction ? View.VISIBLE : View.GONE);
        binding.changeStoreAction.setText(
                presentation.chooseOrganization()
                        ? R.string.shell_choose_store_button
                        : R.string.shell_change_store_button);
        return presentation.chooseOrganization()
                ? ScreenAction.SELECT_ORGANIZATION
                : ScreenAction.NONE;
    }

    void requestId(Optional<String> requestId) {
        binding.requestId.setVisibility(requestId.isPresent() ? View.VISIBLE : View.GONE);
        requestId.ifPresent(value -> binding.requestId.setText(
                binding.getRoot().getContext().getString(
                        R.string.auth_request_id,
                        value)));
    }

    void hideShell() {
        binding.shellRoot.setVisibility(View.GONE);
    }

    private void account(AuthenticatedSession session) {
        String name = session.user().name();
        binding.accountName.setText(name == null || name.isBlank()
                ? binding.getRoot().getContext().getString(R.string.auth_account_fallback)
                : name);
        binding.accountEmail.setText(session.user().email() == null
                ? ""
                : session.user().email());
    }

    private void headline(int title, int message, boolean busy) {
        binding.title.setText(title);
        binding.description.setText(message);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    private void primary(boolean visible, int label) {
        binding.primaryAction.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            binding.primaryAction.setText(label);
        }
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "3V";
        }
        int separator = fullName.trim().indexOf(' ');
        return separator < 0
                ? fullName.trim()
                : fullName.trim().substring(0, separator);
    }
}
