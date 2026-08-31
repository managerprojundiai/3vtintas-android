package br.com.tresvtintas.mobile.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;
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
 * Read-only click proof for a salesperson authenticated against the private staging backend.
 *
 * <p>The flow starts from the real role-aware shell. It proves the effective role and store shown
 * to the user, opens each primary operational destination through visible navigation, and confirms
 * that Operations resolves to Deliveries instead of the restricted Attendance area.
 */
@RunWith(AndroidJUnit4.class)
public final class ConnectedSalespersonPhysicalSmokeTest {
    private static final String LOG_TAG = "SalesPhysicalPilot";
    private static final String PILOT_ARGUMENT = "physicalPilot";
    private static final String PILOT_ENABLED = "true";
    private static final long SCREEN_TIMEOUT_MILLIS = 20_000L;
    private static final long RETRY_INTERVAL_MILLIS = 200L;

    @Test
    public void authenticatedSalespersonScopeIsVisible() {
        prepareSalespersonShell();
        assertTrue(
                "Physical pilot requires the private API.",
                BuildConfig.MOBILE_API_CONFIGURED);
        logCheckpoint("salesperson-scope-visible");
    }

    @Test
    public void salespersonOpensOrdersByClicks() {
        prepareSalespersonShell();
        openPrimaryDestination(
                "Vendas",
                br.com.tresvtintas.mobile.feature.order.R.id.order_list_root,
                "Pedidos");
        waitUntilNotDisplayed(
                withId(br.com.tresvtintas.mobile.feature.order.R.id.order_progress),
                "orders loading indicator");
        onView(withId(br.com.tresvtintas.mobile.feature.order.R.id.order_retry))
                .check(matches(not(isDisplayed())));
        logCheckpoint("orders-opened-by-click");
    }

    @Test
    public void salespersonOpensAuthorizedAgendaByClicks() {
        prepareSalespersonShell();
        openPrimaryDestination(
                "Agenda",
                br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_month_title,
                "Agenda integrada");
        waitUntilDisplayed(
                withId(br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_calendar),
                "agenda calendar");
        waitUntilNotDisplayed(
                withId(br.com.tresvtintas.mobile.feature.appointment.R.id.appointment_progress),
                "agenda loading indicator");
        onView(withId(br.com.tresvtintas.mobile.feature.appointment.R.id.appointment_retry))
                .check(matches(not(isDisplayed())));
        onView(withText("Seu acesso à agenda foi alterado. Volte à tela inicial."))
                .check(doesNotExist());
        logCheckpoint("agenda-opened-by-click");
    }

    @Test
    public void salespersonOpensDeliveriesWithoutAttendanceByClicks() {
        prepareSalespersonShell();
        openPrimaryDestination(
                "Operação",
                br.com.tresvtintas.mobile.feature.delivery.R.id.delivery_list_root,
                "Entregas");
        waitUntilNotDisplayed(
                withId(br.com.tresvtintas.mobile.feature.delivery.R.id.delivery_progress),
                "deliveries loading indicator");
        onView(withId(br.com.tresvtintas.mobile.feature.delivery.R.id.delivery_retry))
                .check(matches(not(isDisplayed())));
        onView(withText("Seu acesso a esta entrega foi revogado."))
                .check(doesNotExist());
        onView(withText("Atendimento")).check(doesNotExist());
        logCheckpoint("deliveries-opened-without-attendance");
    }

    @Test
    public void salespersonMoreSheetOmitsRestrictedAdminActions() {
        prepareSalespersonShell();
        onView(withContentDescription("Mais")).perform(click());
        waitUntilDisplayed(withText("Menu 3V Tintas"), "role-aware More sheet");
        onView(withText("Atendimento")).check(doesNotExist());
        onView(withText("Usuários e acessos")).check(doesNotExist());
        onView(withText("Pintores e aprovações")).check(doesNotExist());
        logCheckpoint("restricted-admin-actions-absent");
        logCheckpoint("passed");
    }

    private static void openPrimaryDestination(
            String navigationLabel,
            int destinationViewId,
            String destinationTitle) {
        waitUntilDisplayed(
                withContentDescription(navigationLabel),
                navigationLabel + " navigation action");
        onView(withContentDescription(navigationLabel)).perform(click());
        waitUntilDisplayed(
                withId(destinationViewId),
                destinationTitle + " screen");
        onView(withId(destinationViewId)).check(matches(isDisplayed()));
    }

    private static void prepareSalespersonShell() {
        assumeTrue(
                "Physical pilot smoke requires an explicitly configured API endpoint.",
                BuildConfig.MOBILE_API_CONFIGURED);
        assumeTrue(
                "Physical pilot smoke is opt-in and must not run in ordinary gates.",
                PILOT_ENABLED.equals(InstrumentationRegistry.getArguments()
                        .getString(PILOT_ARGUMENT)));
        launchAuthorizedShell();
        waitUntilDisplayed(
                withId(R.id.shell_role),
                "salesperson role");
        onView(withId(R.id.shell_role)).check(matches(withText("Vendedor")));
        waitUntilDisplayed(
                withId(R.id.shell_scope),
                "salesperson store scope");
        onView(withId(R.id.shell_scope)).check(matches(
                withText(startsWith("Loja selecionada: Jundiai"))));
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
            String description) {
        long deadline = SystemClock.elapsedRealtime() + SCREEN_TIMEOUT_MILLIS;
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

    private static void waitUntilNotDisplayed(
            Matcher<View> matcher,
            String description) {
        long deadline = SystemClock.elapsedRealtime() + SCREEN_TIMEOUT_MILLIS;
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                onView(matcher).check(matches(not(isDisplayed())));
                return;
            } catch (NoMatchingViewException absent) {
                return;
            } catch (NoActivityResumedException | AssertionError retryable) {
                SystemClock.sleep(RETRY_INTERVAL_MILLIS);
            }
        }
        fail("Timed out waiting for " + description + " to finish.");
    }

    private static void logCheckpoint(String checkpoint) {
        Log.i(LOG_TAG, checkpoint);
    }
}
