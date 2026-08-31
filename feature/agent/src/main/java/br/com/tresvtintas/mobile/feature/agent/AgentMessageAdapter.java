package br.com.tresvtintas.mobile.feature.agent;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.core.agent.AgentActionResult;
import br.com.tresvtintas.mobile.core.agent.AgentActionStatus;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceChannel;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplySummary;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionSummary;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendResult;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateResult;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteSendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMessage;
import br.com.tresvtintas.mobile.core.agent.AgentMessageRole;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentItemMessageBinding;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class AgentMessageAdapter
        extends ListAdapter<AgentMessage, AgentMessageAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AgentMessage>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AgentMessage oldItem,
                        @NonNull AgentMessage newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AgentMessage oldItem,
                        @NonNull AgentMessage newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AgentDocumentNavigator documentNavigator;
    private final AgentQuoteNavigator quoteNavigator;
    private final AgentActionReviewListener actionReviewer;
    private Optional<String> busyActionId = Optional.empty();

    AgentMessageAdapter(
            AgentDocumentNavigator documentNavigator,
            AgentQuoteNavigator quoteNavigator,
            AgentActionReviewListener actionReviewer) {
        super(DIFFERENCE);
        this.documentNavigator = Objects.requireNonNull(
                documentNavigator,
                "Agent document navigator is required.");
        this.quoteNavigator = Objects.requireNonNull(
                quoteNavigator,
                "Agent quote navigator is required.");
        this.actionReviewer = Objects.requireNonNull(
                actionReviewer,
                "Agent action reviewer is required.");
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(
                AgentItemMessageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false),
                documentNavigator,
                quoteNavigator,
                actionReviewer);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(
                getItem(position),
                busyActionId);
    }

    void setBusyActionId(Optional<String> actionId) {
        Optional<String> required = Objects.requireNonNull(
                actionId,
                "Agent busy action is required.");
        Optional<String> previous = busyActionId;
        busyActionId = required;
        previous.ifPresent(this::refreshAction);
        required.filter(value -> !previous.filter(
                        value::equals).isPresent())
                .ifPresent(this::refreshAction);
    }

    private void refreshAction(String actionId) {
        for (int index = 0; index < getCurrentList().size(); index++) {
            boolean matches = getCurrentList().get(index)
                    .actions()
                    .stream()
                    .anyMatch(action -> action.id().equals(actionId));
            if (matches) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AgentItemMessageBinding binding;
        private final AgentDocumentNavigator documentNavigator;
        private final AgentQuoteNavigator quoteNavigator;
        private final AgentActionReviewListener actionReviewer;

        Holder(
                AgentItemMessageBinding binding,
                AgentDocumentNavigator documentNavigator,
                AgentQuoteNavigator quoteNavigator,
                AgentActionReviewListener actionReviewer) {
            super(binding.getRoot());
            this.binding = binding;
            this.documentNavigator = documentNavigator;
            this.quoteNavigator = quoteNavigator;
            this.actionReviewer = actionReviewer;
        }

        void bind(
                AgentMessage value,
                Optional<String> busyActionId) {
            boolean user = value.role() == AgentMessageRole.USER;
            binding.agentMessageRow.setGravity(
                    user ? Gravity.END : Gravity.START);
            binding.agentMessageRole.setText(
                    user
                            ? R.string.agent_message_user
                            : R.string.agent_message_assistant);
            binding.agentMessageContent.setText(value.content());
            value.documents().stream().findFirst().ifPresentOrElse(
                    document -> {
                        binding.agentMessageDocument.setVisibility(
                                View.VISIBLE);
                        binding.agentMessageDocument.setText(
                                document.type()
                                                == br.com.tresvtintas.mobile.core.agent.AgentDocumentType
                                                        .MATERIAL_QUOTE_PDF
                                        ? R.string.agent_document_material_pdf
                                        : R.string.agent_document_labor_pdf);
                        binding.agentMessageDocument.setOnClickListener(
                                view -> documentNavigator.open(
                                        view.getContext(),
                                        document));
                    },
                    () -> {
                        binding.agentMessageDocument.setVisibility(
                                View.GONE);
                        binding.agentMessageDocument.setOnClickListener(
                                null);
                    });
            value.actions().stream().findFirst().ifPresentOrElse(
                    action -> bindAction(action, busyActionId),
                    this::clearAction);
            binding.agentMessageTime.setText(
                    AgentText.activity(value.createdAt()));
        }

        private void bindAction(
                AgentAction action,
                Optional<String> busyActionId) {
            boolean busy = busyActionId.filter(
                            action.id()::equals)
                    .isPresent();
            boolean pending = action.canDecide(Instant.now());
            AgentActionStatus status = action.status()
                            == AgentActionStatus.PENDING
                    && !pending
                    ? AgentActionStatus.EXPIRED
                    : action.status();
            binding.agentMessageActionCard.setVisibility(View.VISIBLE);
            binding.agentMessageActionTitle.setText(action.title());
            NumberFormat currency = NumberFormat.getCurrencyInstance(
                    new Locale("pt", "BR"));
            binding.agentMessageActionSummary.setText(
                    actionSummary(action, currency));
            binding.agentMessageActionRevalidation.setText(
                    revalidationNotice(action.kind()));
            binding.agentMessageActionStatus.setText(
                    statusLabel(status, busy, action.kind()));
            Optional<Long> navigableQuoteId =
                    navigableQuoteId(action.result());
            binding.agentMessageActionReview.setVisibility(
                    pending || navigableQuoteId.isPresent()
                            ? View.VISIBLE
                            : View.GONE);
            binding.agentMessageActionReview.setEnabled(!busy);
            binding.agentMessageActionReview.setText(pending
                    ? reviewLabel(action.kind())
                    : R.string.agent_action_open_quote);
            binding.agentMessageActionReview.setOnClickListener(
                    pending
                            ? view -> actionReviewer.review(action)
                            : navigableQuoteId
                                    .<View.OnClickListener>map(quoteId ->
                                            view -> quoteNavigator.open(
                                                    view.getContext(),
                                                    quoteId))
                                    .orElse(null));
        }

        private String actionSummary(
                AgentAction action,
                NumberFormat currency) {
            if (action.summary()
                    instanceof AgentAttendanceReplySummary attendance) {
                String channel = binding.getRoot()
                        .getContext()
                        .getString(
                                attendance.channel()
                                                == AgentAttendanceChannel
                                                        .WHATSAPP
                                        ? R.string
                                                .agent_action_attendance_channel_whatsapp
                                        : R.string
                                                .agent_action_attendance_channel_site_chat);
                return binding.getRoot().getContext().getString(
                        R.string.agent_action_attendance_summary,
                        attendance.customerName(),
                        channel);
            }
            if (action.summary()
                    instanceof AgentAppointmentSummary appointment) {
                return AgentAppointmentText.compact(
                        binding.getRoot().getContext(),
                        appointment);
            }
            if (action.summary()
                    instanceof AgentDeliverySummary delivery) {
                return AgentDeliveryText.compact(
                        binding.getRoot().getContext(),
                        delivery);
            }
            if (action.summary()
                    instanceof AgentOrderSummary order) {
                return AgentOrderText.compact(
                        binding.getRoot().getContext(),
                        order);
            }
            if (action.summary()
                    instanceof AgentFinanceSummary finance) {
                return binding.getRoot().getContext().getString(
                        R.string.agent_action_finance_summary,
                        finance.title(),
                        currency.format(finance.amount()));
            }
            if (action.summary()
                    instanceof AgentCommissionSummary commission) {
                return binding.getRoot().getContext().getString(
                        R.string.agent_action_commission_summary,
                        commission.recipientName(),
                        currency.format(commission.amount()));
            }
            if (action.summary()
                    instanceof AgentMaterialQuoteCreateSummary create) {
                return binding.getRoot().getContext().getString(
                        R.string.agent_action_create_summary,
                        create.quoteTitle(),
                        create.customerName(),
                        binding.getRoot()
                                .getResources()
                                .getQuantityString(
                                        R.plurals
                                                .agent_action_create_item_count,
                                        create.itemCount(),
                                        create.itemCount()),
                        currency.format(create.total()));
            }
            if (action.summary()
                    instanceof AgentMaterialQuoteAmendSummary amend) {
                return binding.getRoot().getContext().getString(
                        R.string.agent_action_amend_summary,
                        amend.quoteTitle(),
                        amend.customerName(),
                        binding.getRoot()
                                .getResources()
                                .getQuantityString(
                                        R.plurals
                                                .agent_action_amend_change_count,
                                        amend.changes().size(),
                                        amend.changes().size()),
                        currency.format(amend.before().total()),
                        currency.format(amend.after().total()));
            }
            AgentMaterialQuoteSendSummary send =
                    (AgentMaterialQuoteSendSummary) action.summary();
            return binding.getRoot().getContext().getString(
                    R.string.agent_action_send_summary,
                    send.quoteTitle(),
                    send.customerName(),
                    currency.format(send.total()));
        }

        private static Optional<Long> navigableQuoteId(
                Optional<AgentActionResult> result) {
            return result.flatMap(value -> {
                if (value
                        instanceof AgentMaterialQuoteCreateResult create) {
                    return Optional.of(create.quoteId());
                }
                if (value
                        instanceof AgentMaterialQuoteAmendResult amend) {
                    return Optional.of(amend.quoteId());
                }
                return Optional.empty();
            });
        }

        private int statusLabel(
                AgentActionStatus status,
                boolean busy,
                AgentActionKind kind) {
            if (busy) {
                return R.string.agent_action_processing;
            }
            return switch (status) {
                case PENDING -> R.string.agent_action_pending;
                case EXECUTING -> R.string.agent_action_processing;
                case EXECUTED -> executedLabel(kind);
                case REJECTED -> R.string.agent_action_rejected;
                case EXPIRED -> R.string.agent_action_expired;
                case SUPERSEDED -> R.string.agent_action_superseded;
            };
        }

        private int revalidationNotice(AgentActionKind kind) {
            return switch (kind) {
                case MATERIAL_QUOTE_SEND ->
                        R.string.agent_action_send_revalidation_notice;
                case MATERIAL_QUOTE_CREATE ->
                        R.string.agent_action_create_revalidation_notice;
                case MATERIAL_QUOTE_AMEND ->
                        R.string.agent_action_amend_revalidation_notice;
                case ATTENDANCE_REPLY ->
                        R.string.agent_action_attendance_revalidation_notice;
                case APPOINTMENT_CREATE ->
                        R.string
                                .agent_action_appointment_create_revalidation_notice;
                case APPOINTMENT_RESCHEDULE ->
                        R.string
                                .agent_action_appointment_reschedule_revalidation_notice;
                case APPOINTMENT_CANCEL ->
                        R.string
                                .agent_action_appointment_cancel_revalidation_notice;
                case DELIVERY_START ->
                        R.string
                                .agent_action_delivery_start_revalidation_notice;
                case DELIVERY_COMPLETE ->
                        R.string
                                .agent_action_delivery_complete_revalidation_notice;
                case ORDER_CONFIRM ->
                        R.string.agent_action_order_confirm_revalidation_notice;
                case ORDER_START_FULFILLMENT ->
                        R.string
                                .agent_action_order_start_fulfillment_revalidation_notice;
                case ORDER_COMPLETE ->
                        R.string.agent_action_order_complete_revalidation_notice;
                case PERSONAL_FINANCE_CREATE,
                        PERSONAL_FINANCE_SETTLE,
                        PERSONAL_FINANCE_CANCEL,
                        CORPORATE_FINANCE_CREATE,
                        CORPORATE_FINANCE_SETTLE,
                        CORPORATE_FINANCE_CANCEL ->
                        R.string.agent_action_finance_revalidation_notice;
                case COMMISSION_APPROVE,
                        COMMISSION_CANCEL,
                        COMMISSION_PAY ->
                        R.string.agent_action_commission_revalidation_notice;
            };
        }

        private int reviewLabel(AgentActionKind kind) {
            return switch (kind) {
                case MATERIAL_QUOTE_SEND ->
                        R.string.agent_action_review_send;
                case MATERIAL_QUOTE_CREATE ->
                        R.string.agent_action_review_create;
                case MATERIAL_QUOTE_AMEND ->
                        R.string.agent_action_review_amend;
                case ATTENDANCE_REPLY ->
                        R.string.agent_action_review_attendance;
                case APPOINTMENT_CREATE ->
                        R.string.agent_action_review_appointment_create;
                case APPOINTMENT_RESCHEDULE ->
                        R.string.agent_action_review_appointment_reschedule;
                case APPOINTMENT_CANCEL ->
                        R.string.agent_action_review_appointment_cancel;
                case DELIVERY_START ->
                        R.string.agent_action_review_delivery_start;
                case DELIVERY_COMPLETE ->
                        R.string.agent_action_review_delivery_complete;
                case ORDER_CONFIRM ->
                        R.string.agent_action_review_order_confirm;
                case ORDER_START_FULFILLMENT ->
                        R.string
                                .agent_action_review_order_start_fulfillment;
                case ORDER_COMPLETE ->
                        R.string.agent_action_review_order_complete;
                case PERSONAL_FINANCE_CREATE,
                        PERSONAL_FINANCE_SETTLE,
                        PERSONAL_FINANCE_CANCEL,
                        CORPORATE_FINANCE_CREATE,
                        CORPORATE_FINANCE_SETTLE,
                        CORPORATE_FINANCE_CANCEL,
                        COMMISSION_APPROVE,
                        COMMISSION_CANCEL,
                        COMMISSION_PAY ->
                        R.string.agent_action_review_protected;
            };
        }

        private int executedLabel(AgentActionKind kind) {
            return switch (kind) {
                case MATERIAL_QUOTE_SEND ->
                        R.string.agent_action_sent;
                case MATERIAL_QUOTE_CREATE ->
                        R.string.agent_action_created;
                case MATERIAL_QUOTE_AMEND ->
                        R.string.agent_action_amended;
                case ATTENDANCE_REPLY ->
                        R.string.agent_action_attendance_sent;
                case APPOINTMENT_CREATE ->
                        R.string.agent_action_appointment_created;
                case APPOINTMENT_RESCHEDULE ->
                        R.string.agent_action_appointment_rescheduled;
                case APPOINTMENT_CANCEL ->
                        R.string.agent_action_appointment_cancelled;
                case DELIVERY_START ->
                        R.string.agent_action_delivery_started;
                case DELIVERY_COMPLETE ->
                        R.string.agent_action_delivery_completed;
                case ORDER_CONFIRM ->
                        R.string.agent_action_order_confirmed;
                case ORDER_START_FULFILLMENT ->
                        R.string.agent_action_order_fulfillment_started;
                case ORDER_COMPLETE ->
                        R.string.agent_action_order_completed;
                case PERSONAL_FINANCE_CREATE,
                        CORPORATE_FINANCE_CREATE ->
                        R.string.agent_action_finance_created;
                case PERSONAL_FINANCE_SETTLE,
                        CORPORATE_FINANCE_SETTLE ->
                        R.string.agent_action_finance_settled;
                case PERSONAL_FINANCE_CANCEL,
                        CORPORATE_FINANCE_CANCEL ->
                        R.string.agent_action_finance_cancelled;
                case COMMISSION_APPROVE ->
                        R.string.agent_action_commission_approved;
                case COMMISSION_CANCEL ->
                        R.string.agent_action_commission_cancelled;
                case COMMISSION_PAY ->
                        R.string.agent_action_commission_paid;
            };
        }

        private void clearAction() {
            binding.agentMessageActionCard.setVisibility(View.GONE);
            binding.agentMessageActionReview.setOnClickListener(null);
            binding.agentMessageActionReview.setEnabled(false);
        }
    }
}
