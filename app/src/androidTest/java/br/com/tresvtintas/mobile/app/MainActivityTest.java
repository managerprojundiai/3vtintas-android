package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MainActivityTest {
    @Rule
    public final ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(targetActivityIntent());

    @Test
    public void authShellFailsClosedUntilStagingEndpointIsConfigured() {
        activityRule.getScenario().onActivity(activity -> {
            TextView title = activity.findViewById(R.id.title);
            assertTrue(title.isShown());
            assertEquals(
                    activity.getString(R.string.auth_configuration_title),
                    title.getText().toString());
            assertFalse(
                    "Unconfigured build must not expose a false login action.",
                    activity.findViewById(R.id.primary_action).isShown());
            assertFalse(
                    "Unconfigured build must not expose an authorization shell.",
                    activity.findViewById(R.id.shell_root).isShown());
            ViewGroup navigation =
                    activity.findViewById(R.id.shell_bottom_navigation);
            assertFalse(
                    "Unconfigured build must never expose role-aware navigation.",
                    navigation.isShown());
            assertEquals(
                    "Unconfigured build must not materialize protected actions.",
                    0,
                    navigation.getChildCount());
        });
    }

    private static Intent targetActivityIntent() {
        return new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MainActivity.class);
    }
}
