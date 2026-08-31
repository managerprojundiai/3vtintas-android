package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentActionDto(
        String id,
        String kind,
        String status,
        String title,
        AgentActionSummaryDto summary,
        AgentActionResultDto result,
        boolean requiresStepUp,
        String expiresAt) {
    private static final Set<String> STATUSES = Set.of(
            "pending",
            "executing",
            "executed",
            "rejected",
            "expired",
            "superseded");
    private static final String MATERIAL_QUOTE_SEND =
            "material_quote_send";
    private static final String MATERIAL_QUOTE_SEND_TITLE =
            "Enviar orçamento de material";
    private static final String MATERIAL_QUOTE_CREATE =
            "material_quote_create";
    private static final String MATERIAL_QUOTE_CREATE_TITLE =
            "Criar orçamento de material";
    private static final String MATERIAL_QUOTE_AMEND =
            "material_quote_amend";
    private static final String MATERIAL_QUOTE_AMEND_TITLE =
            "Alterar orçamento de material";
    private static final String ATTENDANCE_REPLY =
            "attendance_reply";
    private static final String ATTENDANCE_REPLY_TITLE =
            "Enviar resposta de atendimento";
    private static final String APPOINTMENT_CREATE =
            "appointment_create";
    private static final String APPOINTMENT_CREATE_TITLE =
            "Criar compromisso";
    private static final String APPOINTMENT_RESCHEDULE =
            "appointment_reschedule";
    private static final String APPOINTMENT_RESCHEDULE_TITLE =
            "Reagendar compromisso";
    private static final String APPOINTMENT_CANCEL =
            "appointment_cancel";
    private static final String APPOINTMENT_CANCEL_TITLE =
            "Cancelar compromisso";
    private static final String DELIVERY_START =
            "delivery_start";
    private static final String DELIVERY_START_TITLE =
            "Iniciar entrega";
    private static final String DELIVERY_COMPLETE =
            "delivery_complete";
    private static final String DELIVERY_COMPLETE_TITLE =
            "Concluir entrega";
    private static final String ORDER_CONFIRM =
            "order_confirm";
    private static final String ORDER_CONFIRM_TITLE =
            "Confirmar pedido";
    private static final String ORDER_START_FULFILLMENT =
            "order_start_fulfillment";
    private static final String ORDER_START_FULFILLMENT_TITLE =
            "Iniciar separação do pedido";
    private static final String ORDER_COMPLETE =
            "order_complete";
    private static final String ORDER_COMPLETE_TITLE =
            "Concluir pedido";
    private static final Set<String> STEP_UP_KINDS = Set.of(
            "personal_finance_create",
            "personal_finance_settle",
            "personal_finance_cancel",
            "corporate_finance_create",
            "corporate_finance_settle",
            "corporate_finance_cancel",
            "commission_approve",
            "commission_cancel",
            "commission_pay");

    public AgentActionDto(
            String id,
            String kind,
            String status,
            String title,
            AgentActionSummaryDto summary,
            AgentActionResultDto result,
            String expiresAt) {
        this(
                id,
                kind,
                status,
                title,
                summary,
                result,
                false,
                expiresAt);
    }

    public AgentActionDto {
        id = DtoValidation.requireUuid(id, "Agent action ID");
        if (!STATUSES.contains(status)
                || !validPresentation(kind, title, summary)
                || !validResult(kind, status, result)
                || requiresStepUp != STEP_UP_KINDS.contains(kind)) {
            throw new IllegalArgumentException(
                    "Agent action is invalid.");
        }
        expiresAt = DtoValidation.requireInstant(
                expiresAt,
                "Agent action expiry");
    }

    private static boolean validPresentation(
            String kind,
            String title,
            AgentActionSummaryDto summary) {
        return summary != null
                && summary.validFor(kind)
                && (MATERIAL_QUOTE_SEND.equals(kind)
                                && MATERIAL_QUOTE_SEND_TITLE.equals(title)
                        || MATERIAL_QUOTE_CREATE.equals(kind)
                                && MATERIAL_QUOTE_CREATE_TITLE.equals(title)
                        || MATERIAL_QUOTE_AMEND.equals(kind)
                                && MATERIAL_QUOTE_AMEND_TITLE.equals(title)
                        || ATTENDANCE_REPLY.equals(kind)
                                && ATTENDANCE_REPLY_TITLE.equals(title)
                        || APPOINTMENT_CREATE.equals(kind)
                                && APPOINTMENT_CREATE_TITLE.equals(title)
                        || APPOINTMENT_RESCHEDULE.equals(kind)
                                && APPOINTMENT_RESCHEDULE_TITLE.equals(title)
                        || APPOINTMENT_CANCEL.equals(kind)
                                && APPOINTMENT_CANCEL_TITLE.equals(title)
                        || DELIVERY_START.equals(kind)
                                && DELIVERY_START_TITLE.equals(title)
                        || DELIVERY_COMPLETE.equals(kind)
                                && DELIVERY_COMPLETE_TITLE.equals(title)
                        || ORDER_CONFIRM.equals(kind)
                                && ORDER_CONFIRM_TITLE.equals(title)
                        || ORDER_START_FULFILLMENT.equals(kind)
                                && ORDER_START_FULFILLMENT_TITLE.equals(title)
                        || ORDER_COMPLETE.equals(kind)
                                && ORDER_COMPLETE_TITLE.equals(title)
                        || "personal_finance_create".equals(kind)
                                && "Criar lançamento financeiro pessoal"
                                        .equals(title)
                        || "personal_finance_settle".equals(kind)
                                && "Baixar lançamento financeiro pessoal"
                                        .equals(title)
                        || "personal_finance_cancel".equals(kind)
                                && "Cancelar lançamento financeiro pessoal"
                                        .equals(title)
                        || "corporate_finance_create".equals(kind)
                                && "Criar lançamento financeiro corporativo"
                                        .equals(title)
                        || "corporate_finance_settle".equals(kind)
                                && "Baixar lançamento financeiro corporativo"
                                        .equals(title)
                        || "corporate_finance_cancel".equals(kind)
                                && "Cancelar lançamento financeiro corporativo"
                                        .equals(title)
                        || "commission_approve".equals(kind)
                                && "Aprovar comissão".equals(title)
                        || "commission_cancel".equals(kind)
                                && "Cancelar comissão".equals(title)
                        || "commission_pay".equals(kind)
                                && "Registrar pagamento de comissão"
                                        .equals(title));
    }

    private static boolean validResult(
            String kind,
            String status,
            AgentActionResultDto result) {
        return "executed".equals(status)
                ? result != null && result.validFor(kind)
                : result == null;
    }
}
