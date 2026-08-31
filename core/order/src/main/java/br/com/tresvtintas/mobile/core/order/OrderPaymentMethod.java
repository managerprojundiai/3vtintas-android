package br.com.tresvtintas.mobile.core.order;

import java.util.Locale;

public enum OrderPaymentMethod {
    PIX,
    TRANSFER,
    CASH,
    BANK_SLIP,
    OTHER;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
