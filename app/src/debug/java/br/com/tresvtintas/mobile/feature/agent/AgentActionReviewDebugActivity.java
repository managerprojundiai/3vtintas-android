package br.com.tresvtintas.mobile.feature.agent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.app.R;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;

/**
 * Debug-only physical-device host for the real agent action dialog.
 *
 * <p>The activity is absent from staging and release. It uses synthetic data,
 * never activates repositories and exposes the received decision on screen so
 * an instrumented test can prove the complete touch path.</p>
 */
public final class AgentActionReviewDebugActivity
        extends AppCompatActivity {
    static final String EXTRA_SCENARIO =
            "br.com.tresvtintas.mobile.agent.REVIEW_SCENARIO";
    static final String SCENARIO_ATTENDANCE = "attendance";
    static final String SCENARIO_APPOINTMENT_CREATE =
            "appointment_create";
    static final String SCENARIO_APPOINTMENT_RESCHEDULE =
            "appointment_reschedule";
    static final String SCENARIO_APPOINTMENT_CANCEL =
            "appointment_cancel";
    static final String SCENARIO_DELIVERY_START =
            "delivery_start";
    static final String SCENARIO_DELIVERY_COMPLETE =
            "delivery_complete";
    static final String SCENARIO_ORDER_CONFIRM =
            "order_confirm";
    static final String SCENARIO_ORDER_START_FULFILLMENT =
            "order_start_fulfillment";
    static final String SCENARIO_ORDER_COMPLETE =
            "order_complete";
    private TextView decisionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity_agent_action_review);
        decisionView = findViewById(R.id.debug_agent_action_decision);
        Button reopen = findViewById(R.id.debug_agent_action_reopen);
        reopen.setOnClickListener(ignored -> review());
        reopen.post(this::review);
    }

    private void review() {
        decisionView.setText(R.string.debug_agent_action_no_decision);
        AgentActionDialogs.review(
                this,
                action(),
                this::renderDecision);
    }

    private br.com.tresvtintas.mobile.core.agent.AgentAction action() {
        String scenario = getIntent().getStringExtra(EXTRA_SCENARIO);
        return switch (scenario == null ? "" : scenario) {
            case SCENARIO_ATTENDANCE ->
                    AgentActionReviewFixtures.attendanceReply();
            case SCENARIO_APPOINTMENT_CREATE ->
                    AgentActionReviewFixtures.appointmentCreate();
            case SCENARIO_APPOINTMENT_RESCHEDULE ->
                    AgentActionReviewFixtures.appointmentReschedule();
            case SCENARIO_APPOINTMENT_CANCEL ->
                    AgentActionReviewFixtures.appointmentCancel();
            case SCENARIO_DELIVERY_START ->
                    AgentActionReviewFixtures.deliveryStart();
            case SCENARIO_DELIVERY_COMPLETE ->
                    AgentActionReviewFixtures.deliveryComplete();
            case SCENARIO_ORDER_CONFIRM ->
                    AgentActionReviewFixtures.orderConfirm();
            case SCENARIO_ORDER_START_FULFILLMENT ->
                    AgentActionReviewFixtures.orderStartFulfillment();
            case SCENARIO_ORDER_COMPLETE ->
                    AgentActionReviewFixtures.orderComplete();
            default -> AgentActionReviewFixtures.amend();
        };
    }

    private void renderDecision(AgentActionDecision decision) {
        decisionView.setText(decision == AgentActionDecision.CONFIRM
                ? R.string.debug_agent_action_confirm
                : R.string.debug_agent_action_reject);
    }
}
