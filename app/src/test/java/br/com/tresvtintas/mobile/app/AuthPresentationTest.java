package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.AuthState;
import java.util.Optional;
import org.junit.Test;

public final class AuthPresentationTest {
    @Test
    public void configurationStateNeverOffersAFalseLoginAction() {
        AuthPresentation presentation = AuthPresentation.configurationRequired();

        assertEquals(AuthPresentation.Action.NONE, presentation.primaryAction());
        assertEquals(R.string.auth_configuration_message, presentation.message());
    }

    @Test
    public void configuredStartupFailureIsNotMisreportedAsMissingEndpoint() {
        AuthPresentation presentation =
                AuthPresentation.startupFailure(AuthFailureKind.CONFIGURATION);

        assertEquals(AuthPresentation.Action.NONE, presentation.primaryAction());
        assertEquals(
                R.string.auth_startup_configuration_message,
                presentation.message());
    }

    @Test
    public void canceledChooserKeepsExplicitGoogleButtonAvailable() {
        AuthState state = AuthState.signedOut(Optional.of(AuthFailureKind.CANCELED));

        AuthPresentation presentation = AuthPresentation.from(state);

        assertEquals(AuthPresentation.Action.SIGN_IN, presentation.primaryAction());
        assertEquals(R.string.auth_canceled_message, presentation.message());
    }

    @Test
    public void mandatoryUpdateCannotBeBypassedWithRetry() {
        AuthState state = AuthState.error(new br.com.tresvtintas.mobile.core.auth.AuthException(
                AuthFailureKind.UPDATE_REQUIRED,
                "forced"));

        AuthPresentation presentation = AuthPresentation.from(state);

        assertEquals(AuthPresentation.Action.NONE, presentation.primaryAction());
        assertFalse("Update presentation must not report a busy state.", presentation.busy());
    }
}
