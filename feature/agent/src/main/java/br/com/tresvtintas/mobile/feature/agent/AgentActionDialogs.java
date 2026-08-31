package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class AgentActionDialogs {
    @FunctionalInterface
    interface DecisionListener {
        void decide(AgentActionDecision decision);
    }

    private AgentActionDialogs() {
        throw new AssertionError("No instances.");
    }

    static void review(
            AppCompatActivity activity,
            AgentAction action,
            DecisionListener listener) {
        AgentDialogActionReviewBinding binding =
                AgentDialogActionReviewBinding.inflate(
                        activity.getLayoutInflater());
        new AgentActionReviewRenderer(binding).render(action);
        sizeReviewContent(activity, binding);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(action.title())
                .setView(binding.getRoot())
                .create();
        binding.agentActionReviewConfirm.setText(
                confirmLabel(action.kind()));
        binding.agentActionReviewBack.setOnClickListener(
                ignored -> dialog.dismiss());
        binding.agentActionReviewReject.setOnClickListener(ignored -> {
            dialog.dismiss();
            reject(activity, action, listener);
        });
        binding.agentActionReviewConfirm.setOnClickListener(ignored -> {
            dialog.dismiss();
            listener.decide(AgentActionDecision.CONFIRM);
        });
        dialog.show();
    }

    static void retry(
            AppCompatActivity activity,
            AgentActionDecision decision,
            DecisionListener listener) {
        int label = decision == AgentActionDecision.CONFIRM
                ? R.string.agent_action_retry_confirm
                : R.string.agent_action_retry_reject;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.agent_action_retry_title)
                .setMessage(R.string.agent_action_retry_message)
                .setNegativeButton(
                        R.string.agent_action_back,
                        null)
                .setPositiveButton(
                        label,
                        (dialog, ignored) -> listener.decide(decision))
                .show();
    }

    private static void reject(
            AppCompatActivity activity,
            AgentAction action,
            DecisionListener listener) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.agent_action_reject_title)
                .setMessage(rejectMessage(action.kind()))
                .setNegativeButton(
                        R.string.agent_action_back,
                        null)
                .setPositiveButton(
                        R.string.agent_action_reject_confirm,
                        (dialog, ignored) -> listener.decide(
                                AgentActionDecision.REJECT))
                .show();
    }

    private static int confirmLabel(AgentActionKind kind) {
        return switch (kind) {
            case MATERIAL_QUOTE_SEND ->
                    R.string.agent_action_confirm_send;
            case MATERIAL_QUOTE_CREATE ->
                    R.string.agent_action_confirm_create;
            case MATERIAL_QUOTE_AMEND ->
                    R.string.agent_action_confirm_amend;
            case ATTENDANCE_REPLY ->
                    R.string.agent_action_confirm_attendance;
            case APPOINTMENT_CREATE ->
                    R.string.agent_action_confirm_appointment_create;
            case APPOINTMENT_RESCHEDULE ->
                    R.string.agent_action_confirm_appointment_reschedule;
            case APPOINTMENT_CANCEL ->
                    R.string.agent_action_confirm_appointment_cancel;
            case DELIVERY_START ->
                    R.string.agent_action_confirm_delivery_start;
            case DELIVERY_COMPLETE ->
                    R.string.agent_action_confirm_delivery_complete;
            case ORDER_CONFIRM ->
                    R.string.agent_action_confirm_order;
            case ORDER_START_FULFILLMENT ->
                    R.string.agent_action_confirm_order_start_fulfillment;
            case ORDER_COMPLETE ->
                    R.string.agent_action_confirm_order_complete;
            case PERSONAL_FINANCE_CREATE,
                    CORPORATE_FINANCE_CREATE ->
                    R.string.agent_action_confirm_finance_create;
            case PERSONAL_FINANCE_SETTLE,
                    CORPORATE_FINANCE_SETTLE ->
                    R.string.agent_action_confirm_finance_settle;
            case PERSONAL_FINANCE_CANCEL,
                    CORPORATE_FINANCE_CANCEL ->
                    R.string.agent_action_confirm_finance_cancel;
            case COMMISSION_APPROVE ->
                    R.string.agent_action_confirm_commission_approve;
            case COMMISSION_CANCEL ->
                    R.string.agent_action_confirm_commission_cancel;
            case COMMISSION_PAY ->
                    R.string.agent_action_confirm_commission_pay;
        };
    }

    private static int rejectMessage(AgentActionKind kind) {
        return switch (kind) {
            case MATERIAL_QUOTE_SEND ->
                    R.string.agent_action_reject_send_message;
            case MATERIAL_QUOTE_CREATE ->
                    R.string.agent_action_reject_create_message;
            case MATERIAL_QUOTE_AMEND ->
                    R.string.agent_action_reject_amend_message;
            case ATTENDANCE_REPLY ->
                    R.string.agent_action_reject_attendance_message;
            case APPOINTMENT_CREATE ->
                    R.string.agent_action_reject_appointment_create_message;
            case APPOINTMENT_RESCHEDULE ->
                    R.string
                            .agent_action_reject_appointment_reschedule_message;
            case APPOINTMENT_CANCEL ->
                    R.string.agent_action_reject_appointment_cancel_message;
            case DELIVERY_START ->
                    R.string.agent_action_reject_delivery_start_message;
            case DELIVERY_COMPLETE ->
                    R.string.agent_action_reject_delivery_complete_message;
            case ORDER_CONFIRM ->
                    R.string.agent_action_reject_order_confirm_message;
            case ORDER_START_FULFILLMENT ->
                    R.string
                            .agent_action_reject_order_start_fulfillment_message;
            case ORDER_COMPLETE ->
                    R.string.agent_action_reject_order_complete_message;
            case PERSONAL_FINANCE_CREATE,
                    PERSONAL_FINANCE_SETTLE,
                    PERSONAL_FINANCE_CANCEL,
                    CORPORATE_FINANCE_CREATE,
                    CORPORATE_FINANCE_SETTLE,
                    CORPORATE_FINANCE_CANCEL ->
                    R.string.agent_action_reject_finance_message;
            case COMMISSION_APPROVE,
                    COMMISSION_CANCEL,
                    COMMISSION_PAY ->
                    R.string.agent_action_reject_commission_message;
        };
    }

    private static void sizeReviewContent(
            AppCompatActivity activity,
            AgentDialogActionReviewBinding binding) {
        View activityContent = activity.findViewById(android.R.id.content);
        int availableHeight = activityContent.getHeight();
        if (availableHeight == 0) {
            availableHeight = activity.getResources()
                    .getDisplayMetrics()
                    .heightPixels;
        }
        float heightFraction = activity.getResources().getFraction(
                R.fraction.agent_action_review_content_height,
                1,
                1);
        binding.getRoot().setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(availableHeight * heightFraction)));
    }

}
