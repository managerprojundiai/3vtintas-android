package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapException;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapFailureKind;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapState;
import org.junit.Test;

public final class BootstrapPresentationTest {
    @Test
    public void loadingIsBusyWithoutExposingAnAction() {
        BootstrapPresentation presentation = BootstrapPresentation.from(
                BootstrapState.loading());

        assertTrue("Loading presentation must be busy.", presentation.busy());
        assertFalse(
                "Loading presentation must not expose retry.",
                presentation.retryAllowed());
        assertEquals(
                "Loading title must be rendered.",
                R.string.bootstrap_loading_title,
                presentation.title());
    }

    @Test
    public void transientFailuresAllowRetryButMandatoryUpdateDoesNot() {
        BootstrapPresentation network = presentation(BootstrapFailureKind.NETWORK);
        BootstrapPresentation update = presentation(BootstrapFailureKind.UPDATE_REQUIRED);

        assertTrue("Network failure must allow retry.", network.retryAllowed());
        assertFalse("Mandatory update must not allow retry.", update.retryAllowed());
        assertEquals(
                "Mandatory update message must be specific.",
                R.string.auth_update_message,
                update.message());
    }

    private static BootstrapPresentation presentation(BootstrapFailureKind kind) {
        return BootstrapPresentation.from(BootstrapState.error(
                new BootstrapException(kind, "test")));
    }
}
