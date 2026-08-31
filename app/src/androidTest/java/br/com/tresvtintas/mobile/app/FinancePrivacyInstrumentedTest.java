package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.corporatefinance.CorporateFinanceOrganizationActivity;
import br.com.tresvtintas.mobile.feature.finance.FinanceCreateActivity;
import br.com.tresvtintas.mobile.feature.finance.FinanceDetailActivity;
import br.com.tresvtintas.mobile.feature.finance.FinanceListActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class FinancePrivacyInstrumentedTest {
    @Test
    public void corporateFinanceDirectoryBlocksCaptureAndFailsClosed() {
        try (ActivityScenario<CorporateFinanceOrganizationActivity> scenario =
                     ActivityScenario.launch(
                             intent(CorporateFinanceOrganizationActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Corporate finance directory must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing master authorization must hide global finance.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.corporatefinance.R.id
                                        .corporate_finance_global)
                                .isShown());
            });
        }
    }

    @Test
    public void financeListBlocksCaptureWithoutRuntime() {
        try (ActivityScenario<FinanceListActivity> scenario =
                     ActivityScenario.launch(intent(FinanceListActivity.class))) {
            scenario.onActivity(activity -> assertTrue(
                    "Personal finance list must set FLAG_SECURE.",
                    secure(activity.getWindow().getAttributes().flags)));
        }
    }

    @Test
    public void financeEditorBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<FinanceCreateActivity> scenario =
                     ActivityScenario.launch(intent(FinanceCreateActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Personal finance form must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must disable finance creation.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.finance.R.id
                                        .finance_create_save)
                                .isEnabled());
            });
        }
    }

    @Test
    public void financeDetailBlocksCapture() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        try (ActivityScenario<FinanceDetailActivity> scenario =
                     ActivityScenario.launch(
                             FinanceDetailActivity.intent(context, 701))) {
            scenario.onActivity(activity -> assertTrue(
                    "Personal finance detail must set FLAG_SECURE.",
                    secure(activity.getWindow().getAttributes().flags)));
        }
    }

    private static <T> Intent intent(Class<T> activityClass) {
        return new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                activityClass);
    }

    private static boolean secure(int flags) {
        return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }
}
