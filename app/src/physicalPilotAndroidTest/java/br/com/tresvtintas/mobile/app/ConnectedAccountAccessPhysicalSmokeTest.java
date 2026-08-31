package br.com.tresvtintas.mobile.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.io.InputStream;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Read-only physical smoke for the authenticated account access screen.
 *
 * <p>The test only runs against an explicitly configured staging build and never revokes a device
 * or session. It proves that Android can render both real API page contracts after navigating from
 * the role-aware shell.
 */
@RunWith(AndroidJUnit4.class)
public final class ConnectedAccountAccessPhysicalSmokeTest {
    private static final String LOG_TAG = "AccountPhysicalPilot";
    private static final String PILOT_ARGUMENT = "physicalPilot";
    private static final String PILOT_ENABLED = "true";
    private static final long SCREEN_TIMEOUT_MILLIS = 15_000L;
    private static final long RETRY_INTERVAL_MILLIS = 200L;

    @Test
    public void authorizedShellRendersRealDevicesAndSessions() {
        assumeTrue(
                "Physical pilot smoke requires an explicitly configured API endpoint.",
                BuildConfig.MOBILE_API_CONFIGURED);
        assumeTrue(
                "Physical pilot smoke is opt-in and must not run in ordinary gates.",
                PILOT_ENABLED.equals(InstrumentationRegistry.getArguments()
                        .getString(PILOT_ARGUMENT)));

        launchAuthorizedShell();
        logCheckpoint("waiting-for-account-action");
        waitUntilDisplayed(
                withContentDescription("Conta e aparelhos"),
                SCREEN_TIMEOUT_MILLIS,
                "role-aware account access action");
        onView(withContentDescription("Conta e aparelhos")).perform(click());
        logCheckpoint("account-action-clicked");

        waitUntilDisplayed(
                withText("Segurança da conta"),
                SCREEN_TIMEOUT_MILLIS,
                "account access title");
        waitUntilDisplayed(
                withId(br.com.tresvtintas.mobile.feature.accountaccess.R.id
                        .account_access_item_title),
                SCREEN_TIMEOUT_MILLIS,
                "real device row");
        onView(withId(br.com.tresvtintas.mobile.feature.accountaccess.R.id
                .account_access_devices)).check(matches(isChecked()));
        logCheckpoint("devices-rendered");

        onView(withId(br.com.tresvtintas.mobile.feature.accountaccess.R.id
                .account_access_sessions)).perform(click());
        waitUntilDisplayed(
                withText(startsWith("Sessão ")),
                SCREEN_TIMEOUT_MILLIS,
                "real session row");
        onView(withId(br.com.tresvtintas.mobile.feature.accountaccess.R.id
                .account_access_sessions)).check(matches(isChecked()));
        logCheckpoint("sessions-rendered");
        logCheckpoint("passed");
    }

    private static void launchAuthorizedShell() {
        String packageName = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getPackageName();
        String command = "am start -W -n "
                + packageName
                + '/'
                + MainActivity.class.getName();
        ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command);
        try (InputStream output =
                     new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] buffer = new byte[256];
            int drainedBytes = 0;
            int readBytes = output.read(buffer);
            while (readBytes != -1) {
                drainedBytes += readBytes;
                readBytes = output.read(buffer);
            }
            Log.d(LOG_TAG, "Launch command output drained: " + drainedBytes);
        } catch (IOException exception) {
            throw new AssertionError("Unable to launch the authorized shell.", exception);
        }
    }

    private static void waitUntilDisplayed(
            Matcher<View> matcher,
            long timeoutMillis,
            String description) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (NoActivityResumedException
                    | NoMatchingViewException
                    | AssertionError retryable) {
                SystemClock.sleep(RETRY_INTERVAL_MILLIS);
            }
        }
        fail("Timed out waiting for " + description + '.');
    }

    private static void logCheckpoint(String checkpoint) {
        Log.i(LOG_TAG, checkpoint);
    }
}
