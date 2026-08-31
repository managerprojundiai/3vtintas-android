package br.com.tresvtintas.mobile.core.whatsappadmin;

public enum WhatsAppConnectionStatus {
    PENDING("pending"),
    CONNECTING("connecting"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    ERROR("error"),
    DISABLED("disabled");

    private final String wireValue;

    WhatsAppConnectionStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static WhatsAppConnectionStatus fromWireValue(String value) {
        for (WhatsAppConnectionStatus candidate : values()) {
            if (candidate.wireValue.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("WhatsApp connection status is invalid.");
    }
}
