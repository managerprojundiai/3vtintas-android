package br.com.tresvtintas.mobile.core.appointment;

public enum AgendaEntryType {
    APPOINTMENT,
    DELIVERY,
    COLLECTION,
    RECEIVABLE,
    PAYABLE,
    EXPENSE;

    public boolean isFinancial() {
        return this == RECEIVABLE || this == PAYABLE || this == EXPENSE;
    }
}
