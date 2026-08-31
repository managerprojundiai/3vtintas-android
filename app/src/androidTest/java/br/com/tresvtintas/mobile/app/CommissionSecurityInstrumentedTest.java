package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.feature.commission.CommissionDetailActivity;
import br.com.tresvtintas.mobile.feature.commission.CommissionListActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class CommissionSecurityInstrumentedTest {
    @Test
    public void listBlocksCaptureAndFailsClosedWithoutAuthorization() {
        try (ActivityScenario<CommissionListActivity> scenario =
                     ActivityScenario.launch(new Intent(
                             context(),
                             CommissionListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Commission list must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "An unavailable account scope must hide commission rows.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.commission.R.id
                                        .commission_list)
                                .isShown());
            });
        }
    }

    @Test
    public void detailBlocksCaptureAndHidesEveryMutationByDefault() {
        try (ActivityScenario<CommissionDetailActivity> scenario =
                     ActivityScenario.launch(
                             CommissionDetailActivity.intent(
                                     context(),
                                     701,
                                     CommissionScope.TEAM))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Commission detail must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Approval must fail closed without server authorization.",
                        shown(
                                activity,
                                br.com.tresvtintas.mobile.feature.commission.R.id
                                        .commission_action_approve));
                assertFalse(
                        "Cancellation must fail closed without server authorization.",
                        shown(
                                activity,
                                br.com.tresvtintas.mobile.feature.commission.R.id
                                        .commission_action_cancel));
                assertFalse(
                        "Payment must fail closed without server authorization.",
                        shown(
                                activity,
                                br.com.tresvtintas.mobile.feature.commission.R.id
                                        .commission_action_pay));
            });
        }
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
    }

    private static boolean secure(int flags) {
        return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }

    private static boolean shown(
            CommissionDetailActivity activity,
            int viewId) {
        return activity.findViewById(viewId).isShown();
    }
}
