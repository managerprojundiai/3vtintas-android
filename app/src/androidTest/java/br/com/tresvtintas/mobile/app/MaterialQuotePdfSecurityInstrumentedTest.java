package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuotePdfViewerActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MaterialQuotePdfSecurityInstrumentedTest {
    @Test
    public void pdfProviderIsPrivateAndOnlySupportsTemporaryUriGrants() {
        Context context = targetContext();
        ProviderInfo provider = context.getPackageManager().resolveContentProvider(
                context.getPackageName() + ".fileprovider",
                0);

        assertNotNull("The private PDF provider must be registered.", provider);
        assertFalse("The PDF provider must never be exported.", provider.exported);
        assertTrue(
                "Sharing requires explicit temporary URI grants.",
                provider.grantUriPermissions);
    }

    @Test
    public void pdfViewerBlocksCaptureAndRejectsAnUntrustedLaunch() {
        Intent intent = new Intent(
                targetContext(),
                MaterialQuotePdfViewerActivity.class);
        try (ActivityScenario<MaterialQuotePdfViewerActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int flags = activity.getWindow().getAttributes().flags;
                assertTrue(
                        "Quote PDFs must set FLAG_SECURE.",
                        (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0);
                assertFalse(
                        "An untrusted launch must never expose sharing.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.quote.R.id
                                        .quote_pdf_share)
                                .isEnabled());
            });
        }
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
