package br.com.tresvtintas.mobile.core.order;

import java.util.Locale;

public enum OrderAction {
    CONFIRM,
    START_FULFILLMENT,
    COMPLETE,
    CANCEL,
    RECORD_PAYMENT;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
