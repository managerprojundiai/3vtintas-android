package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.customer.CustomerEditActivity;
import br.com.tresvtintas.mobile.feature.customer.CustomerListActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class CustomerPrivacyTest {
    @Test
    public void customerListBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<CustomerListActivity> scenario =
                     ActivityScenario.launch(intent(CustomerListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Customer PII screen must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertTrue(
                        "Missing runtime must produce a visible fail-closed state.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.customer.R.id
                                        .customer_empty_group)
                                .isShown());
                assertFalse(
                        "Missing authorization never exposes customer creation.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.customer.R.id
                                        .customer_add)
                                .isShown());
            });
        }
    }

    @Test
    public void customerEditorBlocksCaptureAndDisablesWritesWithoutRuntime() {
        try (ActivityScenario<CustomerEditActivity> scenario =
                     ActivityScenario.launch(intent(CustomerEditActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Customer form must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must disable saving.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.customer.R.id
                                        .customer_save)
                                .isEnabled());
            });
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
