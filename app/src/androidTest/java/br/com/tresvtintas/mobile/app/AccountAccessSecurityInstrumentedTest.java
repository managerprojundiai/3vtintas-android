package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AccountAccessSecurityInstrumentedTest {
    @Test
    public void managedAccountAccessBlocksScreenCapture() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        try (ActivityScenario<AccountAccessActivity> scenario =
                     ActivityScenario.launch(
                             AccountAccessActivity.managedIntent(
                                     context,
                                     701,
                                     "Managed test user"))) {
            scenario.onActivity(activity -> assertTrue(
                    "Managed account access must set FLAG_SECURE.",
                    (activity.getWindow().getAttributes().flags
                            & WindowManager.LayoutParams.FLAG_SECURE) != 0));
        }
    }

    @Test
    public void managedIntentRejectsInvalidTargetsBeforeLaunch() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();

        assertInvalidTarget(context, 0, "Managed test user");
        assertInvalidTarget(context, 701, " ");
    }

    @Test
    public void accountAccessActivityIsNotExported()
            throws PackageManager.NameNotFoundException {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        ActivityInfo info = context.getPackageManager()
                .getActivityInfo(
                        new ComponentName(
                                context,
                                AccountAccessActivity.class),
                        PackageManager.GET_META_DATA);

        assertFalse(
                "Account access management must not be externally launched.",
                info.exported);
    }

    private static void assertInvalidTarget(
            Context context,
            long userId,
            String userName) {
        boolean rejected = false;
        try {
            AccountAccessActivity.managedIntent(
                    context,
                    userId,
                    userName);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(
                "Invalid managed account targets must fail before launch.",
                rejected);
    }
}
