package br.com.tresvtintas.mobile.core.whatsappadmin;

public enum WhatsAppStoreStatus {
    PENDING("pending"),
    ACTIVE("active"),
    BLOCKED("blocked");

    private final String wireValue;

    WhatsAppStoreStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static WhatsAppStoreStatus fromWireValue(String value) {
        for (WhatsAppStoreStatus candidate : values()) {
            if (candidate.wireValue.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("WhatsApp store status is invalid.");
    }
}
