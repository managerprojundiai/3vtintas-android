package br.com.tresvtintas.mobile.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Opt-in smoke for an explicitly authorized physical pilot.
 *
 * <p>The normal CI build has no endpoint and never runs this flow. Enabling it requires both a
 * configured application variant and the {@code physicalPilot=true} instrumentation argument.
 */
@RunWith(AndroidJUnit4.class)
public final class ConnectedAgendaPhysicalSmokeTest {
    private static final String LOG_TAG = "AgendaPhysicalPilot";
    private static final String PILOT_ARGUMENT = "physicalPilot";
    private static final String PILOT_ENABLED = "true";
    private static final long SHELL_TIMEOUT_MILLIS = 20_000L;
    private static final long SCREEN_TIMEOUT_MILLIS = 10_000L;
    private static final long RETRY_INTERVAL_MILLIS = 200L;
    private static final Locale PORTUGUESE_BRAZIL =
            new Locale("pt", "BR");
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern(
                    "MMMM 'de' yyyy",
                    PORTUGUESE_BRAZIL);
    private static final DateTimeFormatter ACCESSIBLE_DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(PORTUGUESE_BRAZIL);
    private static final DateTimeFormatter SELECTED_DATE =
            DateTimeFormatter.ofPattern(
                    "EEEE, d 'de' MMMM",
                    PORTUGUESE_BRAZIL);

    @Test
    public void authorizedShellNavigatesAndOperatesMonthlyAgenda() {
        assumeTrue(
                "Physical pilot smoke requires an explicitly configured API endpoint.",
                BuildConfig.MOBILE_API_CONFIGURED);
        assumeTrue(
                "Physical pilot smoke is opt-in and must not run in ordinary gates.",
                PILOT_ENABLED.equals(InstrumentationRegistry.getArguments()
                        .getString(PILOT_ARGUMENT)));

        logCheckpoint("waiting-for-authorized-foreground-launch");
        Matcher<View> agendaNavigation = withContentDescription("Agenda");
        waitUntilDisplayed(agendaNavigation, SHELL_TIMEOUT_MILLIS, "role-aware Agenda action");
        logCheckpoint("agenda-action-visible");
        onView(agendaNavigation).perform(click());
        logCheckpoint("agenda-action-clicked");

        int monthTitle = br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_month_title;
        int previousMonth =
                br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_previous_month;
        int today = br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_today;
        int selectedDate =
                br.com.tresvtintas.mobile.feature.appointment.R.id.agenda_selected_date;
        LocalDate currentDate = LocalDate.now();
        LocalDate targetDate = currentDate.plusDays(1);

        waitUntilDisplayed(
                withText(monthTitle(YearMonth.from(currentDate))),
                SCREEN_TIMEOUT_MILLIS,
                "current Agenda month");
        logCheckpoint("current-month-visible");
        onView(withId(previousMonth)).perform(click());
        waitUntilDisplayed(
                withText(monthTitle(
                        YearMonth.from(currentDate).minusMonths(1))),
                SCREEN_TIMEOUT_MILLIS,
                "previous Agenda month");
        logCheckpoint("previous-month-visible");

        onView(withId(today)).perform(click());
        waitUntilDisplayed(
                withText(monthTitle(YearMonth.from(currentDate))),
                SCREEN_TIMEOUT_MILLIS,
                "Agenda month restored by Today");
        logCheckpoint("today-restored");
        onView(withContentDescription(startsWith(
                ACCESSIBLE_DATE.format(targetDate))))
                .perform(click());
        waitUntilDisplayed(
                withText(titleCase(SELECTED_DATE.format(targetDate))),
                SCREEN_TIMEOUT_MILLIS,
                "selected Agenda day");
        logCheckpoint("selected-date-visible");

        onView(withId(monthTitle)).check(matches(isDisplayed()));
        onView(withId(selectedDate)).check(matches(isDisplayed()));
        logCheckpoint("passed");
    }

    private static String monthTitle(YearMonth month) {
        return titleCase(MONTH.format(month));
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(PORTUGUESE_BRAZIL)
                + value.substring(1);
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
