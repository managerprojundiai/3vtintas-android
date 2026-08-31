package br.com.tresvtintas.mobile.feature.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Matcher;

@RunWith(AndroidJUnit4.class)
public final class AgentActionReviewInstrumentedTest {
    private static final long UI_IDLE_TIMEOUT_MILLIS = 500L;
    private static final long UI_GLOBAL_TIMEOUT_MILLIS = 5_000L;
    private static final ViewAction REAL_BUTTON_CLICK =
            new RealButtonClickAction();
    private static final String VISUAL_AUDIT_DIRECTORY =
            Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath()
                    + "/3v-f9-08-visual-audit/";
    private static final String DELIVERY_VISUAL_AUDIT_DIRECTORY =
            Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath()
                    + "/3v-f9-11-visual-audit/";

    @After
    public void finishDebugReviewActivity() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity
                    : ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)) {
                if (activity
                        instanceof AgentActionReviewDebugActivity) {
                    activity.finishAndRemoveTask();
                }
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void rendersEveryCreationFieldBeforeConfirmation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();

            new AgentActionReviewRenderer(binding).render(
                    AgentActionReviewFixtures.create());

            assertEquals(
                    "The full quote title must be shown.",
                    "Pintura interna",
                    binding.agentActionReviewQuote.getText().toString());
            assertEquals(
                    "Every reviewed line must be rendered.",
                    1,
                    binding.agentActionReviewItems.getChildCount());
            assertEquals(
                    "Validity must be visible before confirmation.",
                    View.VISIBLE,
                    binding.agentActionReviewValidity.getVisibility());
            assertEquals(
                    "Notes must be visible before confirmation.",
                    View.VISIBLE,
                    binding.agentActionReviewNotes.getVisibility());
            assertTrue(
                    "The rounded server total must remain visible.",
                    binding.agentActionReviewTotal
                            .getText()
                            .toString()
                            .contains("24,99"));
            assertEquals(
                    "Creation must explain the transactional revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_create_revalidation_notice),
                    binding.agentActionReviewNotice
                            .getText()
                            .toString());
        });
    }

    @Test
    public void hidesCreationOnlyFieldsForSendReview() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();
            AgentActionReviewRenderer renderer =
                    new AgentActionReviewRenderer(binding);
            renderer.render(AgentActionReviewFixtures.create());

            renderer.render(AgentActionReviewFixtures.send());

            assertEquals(
                    "Send review must not retain creation items.",
                    0,
                    binding.agentActionReviewItems.getChildCount());
            assertEquals(
                    "Send review must hide validity.",
                    View.GONE,
                    binding.agentActionReviewValidity.getVisibility());
            assertEquals(
                    "Send review must hide notes.",
                    View.GONE,
                    binding.agentActionReviewNotes.getVisibility());
            assertEquals(
                    "Send review must hide subtotal.",
                    View.GONE,
                    binding.agentActionReviewSubtotal.getVisibility());
        });
    }

    @Test
    public void rendersEveryAmendmentBeforeAndAfterConfirmation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();

            new AgentActionReviewRenderer(binding).render(
                    AgentActionReviewFixtures.amend());

            assertEquals(
                    "Every requested operation must be visible.",
                    3,
                    binding.agentActionReviewChanges.getChildCount());
            assertEquals(
                    "Every previous line must be visible.",
                    2,
                    binding.agentActionReviewBeforeItems.getChildCount());
            assertEquals(
                    "Every resulting line must be visible.",
                    2,
                    binding.agentActionReviewAfterItems.getChildCount());
            assertTrue(
                    "The previous total must be visually retained.",
                    binding.agentActionReviewBeforeTotal
                            .getText()
                            .toString()
                            .contains("240,00"));
            assertTrue(
                    "The resulting total must be visually retained.",
                    binding.agentActionReviewTotal
                            .getText()
                            .toString()
                            .contains("340,00"));
            assertEquals(
                    "Amendment review must hide creation-only fields.",
                    View.GONE,
                    binding.agentActionReviewValidity.getVisibility());
            assertEquals(
                    "Amendment confirmation must explain revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_amend_revalidation_notice),
                    binding.agentActionReviewNotice
                            .getText()
                            .toString());
        });
    }

    @Test
    public void rendersExactAttendanceReplyAndHidesQuoteFields() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();

            new AgentActionReviewRenderer(binding).render(
                    AgentActionReviewFixtures.attendanceReply());

            assertEquals(
                    "The customer must identify the target conversation.",
                    "Marina Alves",
                    binding.agentActionReviewQuote.getText().toString());
            assertEquals(
                    "The exact outbound content must be reviewable.",
                    "Olá, Marina! Temos a tinta solicitada em estoque. "
                            + "Posso preparar o orçamento para retirada hoje.",
                    binding.agentActionReviewAttendanceReply
                            .getText()
                            .toString());
            assertTrue(
                    "The last inbound message must be visible.",
                    binding.agentActionReviewAttendanceLatest
                            .getText()
                            .toString()
                            .contains("tinta premium branca"));
            assertTrue(
                    "The store must be visible.",
                    binding.agentActionReviewAttendanceOrganization
                            .getText()
                            .toString()
                            .contains("Loja Centro"));
            assertEquals(
                    "Attendance review must hide quote totals.",
                    View.GONE,
                    binding.agentActionReviewTotal.getVisibility());
            assertEquals(
                    "Attendance confirmation must explain fail-closed "
                            + "revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_attendance_revalidation_notice),
                    binding.agentActionReviewNotice
                            .getText()
                            .toString());
        });
    }

    @Test
    public void rendersEveryAppointmentStateBeforeConfirmation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();
            AgentActionReviewRenderer renderer =
                    new AgentActionReviewRenderer(binding);

            renderer.render(AgentActionReviewFixtures.appointmentCreate());
            assertEquals(
                    "Creation must not invent a previous state.",
                    View.GONE,
                    binding.agentActionReviewAppointmentBefore
                            .getVisibility());
            assertTrue(
                    "The responsible person must be explicit.",
                    binding.agentActionReviewAppointmentResponsible
                            .getText()
                            .toString()
                            .contains("Carlos Pereira"));

            renderer.render(
                    AgentActionReviewFixtures.appointmentReschedule());
            assertTrue(
                    "The former schedule must remain visible.",
                    binding.agentActionReviewAppointmentBefore
                            .getText()
                            .toString()
                            .contains("03/08/2026"));
            assertTrue(
                    "The new schedule must remain visible.",
                    binding.agentActionReviewAppointmentAfter
                            .getText()
                            .toString()
                            .contains("04/08/2026"));

            renderer.render(AgentActionReviewFixtures.appointmentCancel());
            assertTrue(
                    "Cancellation must show the terminal state.",
                    binding.agentActionReviewAppointmentAfter
                            .getText()
                            .toString()
                            .contains("Cancelado"));
            assertEquals(
                    "Cancellation must explain fail-closed revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_appointment_cancel_revalidation_notice),
                    binding.agentActionReviewNotice.getText().toString());
        });
    }

    @Test
    public void rendersEveryDeliveryTransitionWithoutPrivateFields() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();
            AgentActionReviewRenderer renderer =
                    new AgentActionReviewRenderer(binding);

            renderer.render(AgentActionReviewFixtures.deliveryStart());
            assertEquals(
                    "The delivery review section must be visible.",
                    View.VISIBLE,
                    binding.agentActionReviewDelivery.getVisibility());
            assertTrue(
                    "The order and delivery identifiers must be explicit.",
                    binding.agentActionReviewDeliveryOrder
                            .getText()
                            .toString()
                            .contains("Pedido #901 • Entrega #81"));
            assertTrue(
                    "The assigned driver must be explicit.",
                    binding.agentActionReviewDeliveryDriver
                            .getText()
                            .toString()
                            .contains("João Entregador"));
            assertTrue(
                    "The previous start state must be visible.",
                    binding.agentActionReviewDeliveryBefore
                            .getText()
                            .toString()
                            .contains("Pendente"));
            assertTrue(
                    "The resulting start state must be visible.",
                    binding.agentActionReviewDeliveryAfter
                            .getText()
                            .toString()
                            .contains("Em trânsito"));
            assertEquals(
                    "Delivery review must hide quote totals.",
                    View.GONE,
                    binding.agentActionReviewTotal.getVisibility());
            assertEquals(
                    "Start must explain fail-closed revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_delivery_start_revalidation_notice),
                    binding.agentActionReviewNotice.getText().toString());

            renderer.render(AgentActionReviewFixtures.deliveryComplete());
            assertTrue(
                    "Completion must retain the in-transit state.",
                    binding.agentActionReviewDeliveryBefore
                            .getText()
                            .toString()
                            .contains("Em trânsito"));
            assertTrue(
                    "Completion must show the terminal state.",
                    binding.agentActionReviewDeliveryAfter
                            .getText()
                            .toString()
                            .contains("Entregue"));
            assertEquals(
                    "Completion must explain fail-closed revalidation.",
                    binding.getRoot().getContext().getString(
                            R.string
                                    .agent_action_delivery_complete_revalidation_notice),
                    binding.agentActionReviewNotice.getText().toString());
        });
    }

    @Test
    public void capturesTheRealAmendmentDialogForVisualAudit()
            throws IOException {
        launchDebugReview();
        onView(withText(R.string.agent_action_changes_label))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        saveScreenshot("f9-08-amend-top.png");
        onView(withId(R.id.agent_action_review_scroll))
                .inRoot(isDialog())
                .perform(swipeUp(), swipeUp(), swipeUp());
        onView(withId(R.id.agent_action_review_total))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        saveScreenshot("f9-08-amend-bottom.png");
        onView(withText(R.string.agent_action_back))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_reopen))
                .check(matches(isDisplayed()));
    }

    @Test
    public void drivesEveryAmendmentDecisionThroughRealButtons()
            throws IOException {
        launchDebugReview();
        onView(withText(R.string.agent_action_back))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_no_decision))
                .check(matches(isDisplayed()));

        reopenReview();
        onView(withText(R.string.agent_action_reject))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(R.string.agent_action_reject_amend_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.agent_action_reject_confirm))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_reject))
                .check(matches(isDisplayed()));

        reopenReview();
        onView(withText(R.string.agent_action_confirm_amend))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm))
                .check(matches(isDisplayed()));
    }

    @Test
    public void confirmsAttendanceOnlyThroughTheRealReviewButton()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity.SCENARIO_ATTENDANCE);
        onView(withText(
                "Olá, Marina! Temos a tinta solicitada em estoque. "
                        + "Posso preparar o orçamento para retirada hoje."))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.agent_action_confirm_attendance))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm))
                .check(matches(isDisplayed()));
    }

    @Test
    public void confirmsAppointmentCreationThroughRealReviewButton()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity
                        .SCENARIO_APPOINTMENT_CREATE);
        onView(withText("Visita técnica — Loja Centro"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(
                R.string.agent_action_confirm_appointment_create))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm);
    }

    @Test
    public void confirmsAppointmentRescheduleThroughRealReviewButton()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity
                        .SCENARIO_APPOINTMENT_RESCHEDULE);
        onView(withId(R.id.agent_action_review_appointment_before))
                .inRoot(isDialog())
                .check(matches(withText(
                        org.hamcrest.Matchers.containsString(
                                "03/08/2026"))));
        onView(withId(R.id.agent_action_review_appointment_after))
                .inRoot(isDialog())
                .check(matches(withText(
                        org.hamcrest.Matchers.containsString(
                                "04/08/2026"))));
        onView(withText(
                R.string.agent_action_confirm_appointment_reschedule))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm);
    }

    @Test
    public void rejectsAppointmentCancellationThroughSecondConfirmation()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity
                        .SCENARIO_APPOINTMENT_CANCEL);
        onView(withText(R.string.agent_action_reject))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                R.string.agent_action_reject_appointment_cancel_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.agent_action_reject_confirm))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_reject);
    }

    @Test
    public void confirmsDeliveryStartThroughTheRealReviewButton()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity.SCENARIO_DELIVERY_START);
        onView(withId(R.id.agent_action_review_delivery_before))
                .inRoot(isDialog())
                .check(matches(withText(
                        org.hamcrest.Matchers.containsString(
                                "Pendente"))));
        onView(withId(R.id.agent_action_review_delivery_after))
                .inRoot(isDialog())
                .check(matches(withText(
                        org.hamcrest.Matchers.containsString(
                                "Em trânsito"))));
        onView(withText(R.string.agent_action_confirm_delivery_start))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm);
    }

    @Test
    public void rejectsDeliveryCompletionThroughSecondConfirmation()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity
                        .SCENARIO_DELIVERY_COMPLETE);
        onView(withText(R.string.agent_action_reject))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                R.string.agent_action_reject_delivery_complete_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.agent_action_reject_confirm))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_reject);
    }

    @Test
    public void capturesBothDeliveryReviewsForVisualAudit()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity.SCENARIO_DELIVERY_START);
        onView(withId(R.id.agent_action_review_delivery))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        saveScreenshot(
                DELIVERY_VISUAL_AUDIT_DIRECTORY,
                "f9-11-delivery-start.png");
        onView(withText(R.string.agent_action_back))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);

        launchDebugReview(
                AgentActionReviewDebugActivity
                        .SCENARIO_DELIVERY_COMPLETE);
        onView(withId(R.id.agent_action_review_delivery))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        saveScreenshot(
                DELIVERY_VISUAL_AUDIT_DIRECTORY,
                "f9-11-delivery-complete.png");
        onView(withText(R.string.agent_action_back))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
    }

    private static void launchDebugReview() throws IOException {
        launchDebugReview(null);
    }

    private static void launchDebugReview(String scenario)
            throws IOException {
        UiAutomation automation = InstrumentationRegistry
                .getInstrumentation()
                .getUiAutomation();
        waitForCommand(automation.executeShellCommand(
                "input keyevent KEYCODE_WAKEUP"));
        waitForCommand(automation.executeShellCommand(
                "wm dismiss-keyguard"));
        String component = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getPackageName()
                + "/"
                + AgentActionReviewDebugActivity.class.getName();
        String scenarioArgument = scenario == null
                ? ""
                : " --es "
                        + AgentActionReviewDebugActivity.EXTRA_SCENARIO
                        + " "
                        + scenario;
        waitForCommand(automation.executeShellCommand(
                "am start -W -f 0x10008000 -n "
                        + component
                        + scenarioArgument));
        waitForStableUi(automation);
    }

    private static void waitForStableUi(UiAutomation automation)
            throws IOException {
        try {
            automation.waitForIdle(
                    UI_IDLE_TIMEOUT_MILLIS,
                    UI_GLOBAL_TIMEOUT_MILLIS);
        } catch (TimeoutException exception) {
            throw new IOException(
                    "The physical review window did not become idle.",
                    exception);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void reopenReview() {
        onView(withText(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_reopen))
                .perform(REAL_BUTTON_CLICK);
    }

    private static void assertDebugDecision(int decisionText) {
        onView(withText(decisionText))
                .check(matches(isDisplayed()));
    }

    private static AgentDialogActionReviewBinding binding() {
        Context context = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext(),
                br.com.tresvtintas.mobile.core.designsystem.R.style
                        .Theme_ThreeVTintas);
        return AgentDialogActionReviewBinding.inflate(
                LayoutInflater.from(context));
    }

    private static void saveScreenshot(String name)
            throws IOException {
        saveScreenshot(VISUAL_AUDIT_DIRECTORY, name);
    }

    private static void saveScreenshot(
            String directory,
            String name) throws IOException {
        UiAutomation automation = InstrumentationRegistry
                .getInstrumentation()
                .getUiAutomation();
        requireSilentCommand(automation.executeShellCommand(
                "mkdir -p " + directory));
        requireSilentCommand(automation.executeShellCommand(
                "screencap -p " + directory + name));
    }

    private static void requireSilentCommand(ParcelFileDescriptor command)
            throws IOException {
        assertEquals(
                "The screenshot shell command must not report output.",
                0,
                drain(command));
    }

    private static void waitForCommand(ParcelFileDescriptor command)
            throws IOException {
        drain(command);
    }

    private static int drain(ParcelFileDescriptor command)
            throws IOException {
        try (InputStream response =
                     new ParcelFileDescriptor.AutoCloseInputStream(command)) {
            byte[] buffer = new byte[256];
            int responseBytes = 0;
            int read = response.read(buffer);
            while (read != -1) {
                responseBytes += read;
                read = response.read(buffer);
            }
            return responseBytes;
        }
    }

    private static final class RealButtonClickAction
            implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            return isDisplayed();
        }

        @Override
        public String getDescription() {
            return "invoke the real visible button";
        }

        @Override
        public void perform(UiController controller, View view) {
            assertTrue(
                    "The real button must accept the click.",
                    view.performClick());
            controller.loopMainThreadUntilIdle();
        }
    }

}
