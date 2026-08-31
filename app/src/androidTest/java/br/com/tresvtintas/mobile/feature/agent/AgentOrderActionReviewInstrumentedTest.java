package br.com.tresvtintas.mobile.feature.agent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AgentOrderActionReviewInstrumentedTest {
    private static final long UI_IDLE_TIMEOUT_MILLIS = 500L;
    private static final long UI_GLOBAL_TIMEOUT_MILLIS = 5_000L;
    private static final ViewAction REAL_BUTTON_CLICK =
            new RealButtonClickAction();
    private static final String VISUAL_AUDIT_DIRECTORY =
            Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath()
                    + "/3v-f9-12-visual-audit/";

    @After
    public void finishDebugReviewActivity() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity
                    : ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)) {
                if (activity instanceof AgentActionReviewDebugActivity) {
                    activity.finishAndRemoveTask();
                }
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void rendersEveryOrderTransitionWithoutPrivateFields() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AgentDialogActionReviewBinding binding = binding();
            AgentActionReviewRenderer renderer =
                    new AgentActionReviewRenderer(binding);

            renderer.render(AgentActionReviewFixtures.orderConfirm());
            assertEquals(
                    "The order review section must be visible.",
                    View.VISIBLE,
                    binding.agentActionReviewOrder.getVisibility());
            assertTrue(
                    "The public order identifier must be explicit.",
                    binding.agentActionReviewQuote
                            .getText()
                            .toString()
                            .contains("Pedido #902"));
            assertTrue(
                    "The customer-facing total must be visible.",
                    binding.agentActionReviewOrderTotal
                            .getText()
                            .toString()
                            .contains("489,90"));
            assertTrue(
                    "The previous pending state must be visible.",
                    binding.agentActionReviewOrderBefore
                            .getText()
                            .toString()
                            .contains("Pendente"));
            assertTrue(
                    "The confirmed target state must be visible.",
                    binding.agentActionReviewOrderAfter
                            .getText()
                            .toString()
                            .contains("Confirmado"));
            assertEquals(
                    "Order review must hide quote-only totals.",
                    View.GONE,
                    binding.agentActionReviewTotal.getVisibility());

            renderer.render(
                    AgentActionReviewFixtures.orderStartFulfillment());
            assertTrue(
                    "Start fulfillment must retain confirmed state.",
                    binding.agentActionReviewOrderBefore
                            .getText()
                            .toString()
                            .contains("Confirmado"));
            assertTrue(
                    "Start fulfillment must show separation state.",
                    binding.agentActionReviewOrderAfter
                            .getText()
                            .toString()
                            .contains("Em separação"));

            renderer.render(AgentActionReviewFixtures.orderComplete());
            assertTrue(
                    "Completion must retain separation state.",
                    binding.agentActionReviewOrderBefore
                            .getText()
                            .toString()
                            .contains("Em separação"));
            assertTrue(
                    "Completion must show the terminal state.",
                    binding.agentActionReviewOrderAfter
                            .getText()
                            .toString()
                            .contains("Concluído"));
        });
    }

    @Test
    public void confirmsOrderOnlyThroughTheRealReviewButton()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity.SCENARIO_ORDER_CONFIRM);
        onView(withId(R.id.agent_action_review_order))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.agent_action_confirm_order))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        assertDebugDecision(
                br.com.tresvtintas.mobile.app.R.string
                        .debug_agent_action_confirm);
    }

    @Test
    public void rejectsOrderCompletionThroughSecondConfirmation()
            throws IOException {
        launchDebugReview(
                AgentActionReviewDebugActivity.SCENARIO_ORDER_COMPLETE);
        onView(withText(R.string.agent_action_reject))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
        onView(withText(
                R.string.agent_action_reject_order_complete_message))
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
    public void capturesAllOrderReviewsForVisualAudit()
            throws IOException {
        assertEquals(
                "The first capture must use the confirmation contract.",
                AgentActionKind.ORDER_CONFIRM,
                AgentActionReviewFixtures.orderConfirm().kind());
        capture(
                AgentActionReviewDebugActivity.SCENARIO_ORDER_CONFIRM,
                "f9-12-order-confirm.png");
        capture(
                AgentActionReviewDebugActivity
                        .SCENARIO_ORDER_START_FULFILLMENT,
                "f9-12-order-start-fulfillment.png");
        capture(
                AgentActionReviewDebugActivity.SCENARIO_ORDER_COMPLETE,
                "f9-12-order-complete.png");
    }

    private static void capture(String scenario, String fileName)
            throws IOException {
        launchDebugReview(scenario);
        onView(withId(R.id.agent_action_review_order))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        saveScreenshot(fileName);
        onView(withText(R.string.agent_action_back))
                .inRoot(isDialog())
                .perform(REAL_BUTTON_CLICK);
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
        waitForCommand(automation.executeShellCommand(
                "am start -W -f 0x10008000 -n "
                        + component
                        + " --es "
                        + AgentActionReviewDebugActivity.EXTRA_SCENARIO
                        + " "
                        + scenario));
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
                    "The physical order review did not become idle.",
                    exception);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
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
        UiAutomation automation = InstrumentationRegistry
                .getInstrumentation()
                .getUiAutomation();
        requireSilentCommand(automation.executeShellCommand(
                "mkdir -p " + VISUAL_AUDIT_DIRECTORY));
        requireSilentCommand(automation.executeShellCommand(
                "screencap -p " + VISUAL_AUDIT_DIRECTORY + name));
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
