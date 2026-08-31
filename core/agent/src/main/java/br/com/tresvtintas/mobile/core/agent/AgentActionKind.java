package br.com.tresvtintas.mobile.core.agent;

public enum AgentActionKind {
    MATERIAL_QUOTE_SEND("material_quote_send", false),
    MATERIAL_QUOTE_CREATE("material_quote_create", false),
    MATERIAL_QUOTE_AMEND("material_quote_amend", false),
    ATTENDANCE_REPLY("attendance_reply", false),
    APPOINTMENT_CREATE("appointment_create", false),
    APPOINTMENT_RESCHEDULE("appointment_reschedule", false),
    APPOINTMENT_CANCEL("appointment_cancel", false),
    DELIVERY_START("delivery_start", false),
    DELIVERY_COMPLETE("delivery_complete", false),
    ORDER_CONFIRM("order_confirm", false),
    ORDER_START_FULFILLMENT("order_start_fulfillment", false),
    ORDER_COMPLETE("order_complete", false),
    PERSONAL_FINANCE_CREATE("personal_finance_create", true),
    PERSONAL_FINANCE_SETTLE("personal_finance_settle", true),
    PERSONAL_FINANCE_CANCEL("personal_finance_cancel", true),
    CORPORATE_FINANCE_CREATE("corporate_finance_create", true),
    CORPORATE_FINANCE_SETTLE("corporate_finance_settle", true),
    CORPORATE_FINANCE_CANCEL("corporate_finance_cancel", true),
    COMMISSION_APPROVE("commission_approve", true),
    COMMISSION_CANCEL("commission_cancel", true),
    COMMISSION_PAY("commission_pay", true);

    private final String wireValue;
    private final boolean requiresStepUp;

    AgentActionKind(String wireValue, boolean requiresStepUp) {
        this.wireValue = wireValue;
        this.requiresStepUp = requiresStepUp;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean requiresStepUp() {
        return requiresStepUp;
    }

    public static AgentActionKind fromWireValue(String value) {
        for (AgentActionKind kind : values()) {
            if (kind.wireValue.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException(
                "Agent action kind is invalid.");
    }
}
