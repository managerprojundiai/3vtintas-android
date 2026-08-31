package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentAction(
        String id,
        AgentActionKind kind,
        AgentActionStatus status,
        String title,
        AgentActionSummary summary,
        Optional<AgentActionResult> result,
        boolean requiresStepUp,
        Instant expiresAt) {
    public static final String MATERIAL_QUOTE_SEND_TITLE =
            "Enviar orçamento de material";
    public static final String MATERIAL_QUOTE_CREATE_TITLE =
            "Criar orçamento de material";
    public static final String MATERIAL_QUOTE_AMEND_TITLE =
            "Alterar orçamento de material";
    public static final String ATTENDANCE_REPLY_TITLE =
            "Enviar resposta de atendimento";
    public static final String APPOINTMENT_CREATE_TITLE =
            "Criar compromisso";
    public static final String APPOINTMENT_RESCHEDULE_TITLE =
            "Reagendar compromisso";
    public static final String APPOINTMENT_CANCEL_TITLE =
            "Cancelar compromisso";
    public static final String DELIVERY_START_TITLE =
            "Iniciar entrega";
    public static final String DELIVERY_COMPLETE_TITLE =
            "Concluir entrega";
    public static final String ORDER_CONFIRM_TITLE =
            "Confirmar pedido";
    public static final String ORDER_START_FULFILLMENT_TITLE =
            "Iniciar separação do pedido";
    public static final String ORDER_COMPLETE_TITLE =
            "Concluir pedido";
    public static final String PERSONAL_FINANCE_CREATE_TITLE =
            "Criar lançamento financeiro pessoal";
    public static final String PERSONAL_FINANCE_SETTLE_TITLE =
            "Baixar lançamento financeiro pessoal";
    public static final String PERSONAL_FINANCE_CANCEL_TITLE =
            "Cancelar lançamento financeiro pessoal";
    public static final String CORPORATE_FINANCE_CREATE_TITLE =
            "Criar lançamento financeiro corporativo";
    public static final String CORPORATE_FINANCE_SETTLE_TITLE =
            "Baixar lançamento financeiro corporativo";
    public static final String CORPORATE_FINANCE_CANCEL_TITLE =
            "Cancelar lançamento financeiro corporativo";
    public static final String COMMISSION_APPROVE_TITLE =
            "Aprovar comissão";
    public static final String COMMISSION_CANCEL_TITLE =
            "Cancelar comissão";
    public static final String COMMISSION_PAY_TITLE =
            "Registrar pagamento de comissão";

    public AgentAction(
            String id,
            AgentActionKind kind,
            AgentActionStatus status,
            String title,
            AgentActionSummary summary,
            Optional<AgentActionResult> result,
            Instant expiresAt) {
        this(
                id,
                Objects.requireNonNull(
                        kind,
                        "Agent action kind is required."),
                status,
                title,
                summary,
                result,
                kind.requiresStepUp(),
                expiresAt);
    }

    public AgentAction {
        id = AgentIdentifiers.requireUuid(
                id,
                "Agent action ID is invalid.");
        kind = Objects.requireNonNull(
                kind,
                "Agent action kind is required.");
        status = Objects.requireNonNull(
                status,
                "Agent action status is required.");
        if (!validPresentation(kind, title, summary)) {
            throw new IllegalArgumentException(
                    "Agent action presentation is invalid.");
        }
        result = Objects.requireNonNull(
                result,
                "Agent action result is required.");
        if (!validResult(kind, status, result)) {
            throw new IllegalArgumentException(
                    "Agent action result is invalid.");
        }
        if (requiresStepUp != kind.requiresStepUp()) {
            throw new IllegalArgumentException(
                    "Agent action step-up policy is invalid.");
        }
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Agent action expiry is required.");
    }

    public boolean canDecide(Instant now) {
        Objects.requireNonNull(now, "Agent action clock is required.");
        return status == AgentActionStatus.PENDING
                && expiresAt.isAfter(now);
    }

    private static boolean validPresentation(
            AgentActionKind kind,
            String title,
            AgentActionSummary summary) {
        Objects.requireNonNull(
                summary,
                "Agent action summary is required.");
        return (kind == AgentActionKind.MATERIAL_QUOTE_SEND
                        && MATERIAL_QUOTE_SEND_TITLE.equals(title)
                        && summary
                                instanceof AgentMaterialQuoteSendSummary)
                || (kind == AgentActionKind.MATERIAL_QUOTE_CREATE
                        && MATERIAL_QUOTE_CREATE_TITLE.equals(title)
                        && summary
                                instanceof AgentMaterialQuoteCreateSummary)
                || (kind == AgentActionKind.MATERIAL_QUOTE_AMEND
                        && MATERIAL_QUOTE_AMEND_TITLE.equals(title)
                        && summary
                                instanceof AgentMaterialQuoteAmendSummary)
                || (kind == AgentActionKind.ATTENDANCE_REPLY
                        && ATTENDANCE_REPLY_TITLE.equals(title)
                        && summary instanceof AgentAttendanceReplySummary)
                || (kind == AgentActionKind.APPOINTMENT_CREATE
                        && APPOINTMENT_CREATE_TITLE.equals(title)
                        && summary instanceof AgentAppointmentSummary value
                        && value.operation()
                                == AgentAppointmentOperation.CREATE)
                || (kind == AgentActionKind.APPOINTMENT_RESCHEDULE
                        && APPOINTMENT_RESCHEDULE_TITLE.equals(title)
                        && summary instanceof AgentAppointmentSummary value
                        && value.operation()
                                == AgentAppointmentOperation.RESCHEDULE)
                || (kind == AgentActionKind.APPOINTMENT_CANCEL
                        && APPOINTMENT_CANCEL_TITLE.equals(title)
                        && summary instanceof AgentAppointmentSummary value
                        && value.operation()
                                == AgentAppointmentOperation.CANCEL)
                || (kind == AgentActionKind.DELIVERY_START
                        && DELIVERY_START_TITLE.equals(title)
                        && summary instanceof AgentDeliverySummary value
                        && value.operation()
                                == AgentDeliveryOperation.START)
                || (kind == AgentActionKind.DELIVERY_COMPLETE
                        && DELIVERY_COMPLETE_TITLE.equals(title)
                        && summary instanceof AgentDeliverySummary value
                        && value.operation()
                                == AgentDeliveryOperation.COMPLETE)
                || (kind == AgentActionKind.ORDER_CONFIRM
                        && ORDER_CONFIRM_TITLE.equals(title)
                        && summary instanceof AgentOrderSummary value
                        && value.operation()
                                == AgentOrderOperation.CONFIRM)
                || (kind == AgentActionKind.ORDER_START_FULFILLMENT
                        && ORDER_START_FULFILLMENT_TITLE.equals(title)
                        && summary instanceof AgentOrderSummary value
                        && value.operation()
                                == AgentOrderOperation.START_FULFILLMENT)
                || (kind == AgentActionKind.ORDER_COMPLETE
                        && ORDER_COMPLETE_TITLE.equals(title)
                        && summary instanceof AgentOrderSummary value
                        && value.operation()
                                == AgentOrderOperation.COMPLETE)
                || validFinancePresentation(kind, title, summary)
                || validCommissionPresentation(kind, title, summary);
    }

    private static boolean validResult(
            AgentActionKind kind,
            AgentActionStatus status,
            Optional<AgentActionResult> result) {
        if (status != AgentActionStatus.EXECUTED) {
            return result.isEmpty();
        }
        return result.filter(value ->
                        (kind == AgentActionKind.MATERIAL_QUOTE_SEND
                                && value
                                        instanceof AgentMaterialQuoteSendResult)
                                || (kind
                                                == AgentActionKind
                                                        .MATERIAL_QUOTE_CREATE
                                        && value
                                                instanceof AgentMaterialQuoteCreateResult)
                                || (kind
                                                == AgentActionKind
                                                        .MATERIAL_QUOTE_AMEND
                                        && value
                                                instanceof AgentMaterialQuoteAmendResult)
                                || (kind
                                                == AgentActionKind
                                                        .ATTENDANCE_REPLY
                                        && value
                                                instanceof AgentAttendanceReplyResult)
                                || ((kind
                                                        == AgentActionKind
                                                                .APPOINTMENT_CREATE
                                                || kind
                                                        == AgentActionKind
                                                                .APPOINTMENT_RESCHEDULE
                                                || kind
                                                        == AgentActionKind
                                                                .APPOINTMENT_CANCEL)
                                        && value
                                                instanceof AgentAppointmentResult)
                                || ((kind
                                                        == AgentActionKind
                                                                .DELIVERY_START
                                                || kind
                                                        == AgentActionKind
                                                                .DELIVERY_COMPLETE)
                                        && value
                                                instanceof AgentDeliveryResult)
                                || ((kind
                                                        == AgentActionKind
                                                                .ORDER_CONFIRM
                                                || kind
                                                        == AgentActionKind
                                                                .ORDER_START_FULFILLMENT
                                                || kind
                                                        == AgentActionKind
                                                                .ORDER_COMPLETE)
                                        && value
                                                instanceof AgentOrderResult)
                                || (isFinance(kind)
                                        && value
                                                instanceof AgentFinanceResult)
                                || (isCommission(kind)
                                        && value
                                                instanceof AgentCommissionResult))
                .isPresent();
    }

    private static boolean validFinancePresentation(
            AgentActionKind kind,
            String title,
            AgentActionSummary summary) {
        if (!(summary instanceof AgentFinanceSummary value)
                || !isFinance(kind)) {
            return false;
        }
        return switch (kind) {
            case PERSONAL_FINANCE_CREATE ->
                    PERSONAL_FINANCE_CREATE_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.PERSONAL
                            && value.operation()
                                    == AgentFinanceOperation.CREATE;
            case PERSONAL_FINANCE_SETTLE ->
                    PERSONAL_FINANCE_SETTLE_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.PERSONAL
                            && value.operation()
                                    == AgentFinanceOperation.SETTLE;
            case PERSONAL_FINANCE_CANCEL ->
                    PERSONAL_FINANCE_CANCEL_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.PERSONAL
                            && value.operation()
                                    == AgentFinanceOperation.CANCEL;
            case CORPORATE_FINANCE_CREATE ->
                    CORPORATE_FINANCE_CREATE_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.CORPORATE
                            && value.operation()
                                    == AgentFinanceOperation.CREATE;
            case CORPORATE_FINANCE_SETTLE ->
                    CORPORATE_FINANCE_SETTLE_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.CORPORATE
                            && value.operation()
                                    == AgentFinanceOperation.SETTLE;
            case CORPORATE_FINANCE_CANCEL ->
                    CORPORATE_FINANCE_CANCEL_TITLE.equals(title)
                            && value.scope()
                                    == AgentFinanceScope.CORPORATE
                            && value.operation()
                                    == AgentFinanceOperation.CANCEL;
            default -> false;
        };
    }

    private static boolean validCommissionPresentation(
            AgentActionKind kind,
            String title,
            AgentActionSummary summary) {
        if (!(summary instanceof AgentCommissionSummary value)
                || !isCommission(kind)) {
            return false;
        }
        return switch (kind) {
            case COMMISSION_APPROVE ->
                    COMMISSION_APPROVE_TITLE.equals(title)
                            && value.operation()
                                    == AgentCommissionOperation.APPROVE;
            case COMMISSION_CANCEL ->
                    COMMISSION_CANCEL_TITLE.equals(title)
                            && value.operation()
                                    == AgentCommissionOperation.CANCEL;
            case COMMISSION_PAY ->
                    COMMISSION_PAY_TITLE.equals(title)
                            && value.operation()
                                    == AgentCommissionOperation.PAY;
            default -> false;
        };
    }

    private static boolean isFinance(AgentActionKind kind) {
        return switch (kind) {
            case PERSONAL_FINANCE_CREATE,
                    PERSONAL_FINANCE_SETTLE,
                    PERSONAL_FINANCE_CANCEL,
                    CORPORATE_FINANCE_CREATE,
                    CORPORATE_FINANCE_SETTLE,
                    CORPORATE_FINANCE_CANCEL -> true;
            default -> false;
        };
    }

    private static boolean isCommission(AgentActionKind kind) {
        return kind == AgentActionKind.COMMISSION_APPROVE
                || kind == AgentActionKind.COMMISSION_CANCEL
                || kind == AgentActionKind.COMMISSION_PAY;
    }
}
