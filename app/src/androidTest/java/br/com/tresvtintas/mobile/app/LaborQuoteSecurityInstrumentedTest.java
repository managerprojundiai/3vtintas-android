package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteEditActivity;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteListActivity;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuotePdfViewerActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class LaborQuoteSecurityInstrumentedTest {
    @Test
    public void listBlocksCaptureAndHidesCreationWithoutRuntime() {
        try (ActivityScenario<LaborQuoteListActivity> scenario =
                     ActivityScenario.launch(intent(LaborQuoteListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue("Labor quote list must block capture.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse("Missing authorization must hide creation.",
                        activity.findViewById(
                        br.com.tresvtintas.mobile.feature.laborquote.R.id
                                .labor_quote_new).isShown());
            });
        }
    }

    @Test
    public void editorBlocksCaptureAndDisablesWritesWithoutRuntime() {
        try (ActivityScenario<LaborQuoteEditActivity> scenario =
                     ActivityScenario.launch(intent(LaborQuoteEditActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue("Labor quote editor must block capture.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse("Missing authorization must disable writes.",
                        activity.findViewById(
                        br.com.tresvtintas.mobile.feature.laborquote.R.id
                                .labor_quote_save).isEnabled());
            });
        }
    }

    @Test
    public void pdfViewerBlocksCaptureAndRejectsUntrustedLaunch() {
        try (ActivityScenario<LaborQuotePdfViewerActivity> scenario =
                     ActivityScenario.launch(intent(LaborQuotePdfViewerActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue("Labor quote PDF viewer must block capture.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse("Untrusted launch must disable sharing.",
                        activity.findViewById(
                        br.com.tresvtintas.mobile.feature.laborquote.R.id
                                .labor_quote_pdf_share).isEnabled());
            });
        }
    }

    private static <T> Intent intent(Class<T> activityClass) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new Intent(context, activityClass);
    }

    private static boolean secure(int flags) {
        return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }
}
