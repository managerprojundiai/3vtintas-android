package br.com.tresvtintas.mobile.core.whatsappadmin;

public enum WhatsAppChannelMode {
    DISABLED("disabled"),
    META("meta"),
    EVOLUTION("evolution"),
    BOTH("both");

    private final String wireValue;

    WhatsAppChannelMode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static WhatsAppChannelMode fromWireValue(String value) {
        for (WhatsAppChannelMode candidate : values()) {
            if (candidate.wireValue.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("WhatsApp channel mode is invalid.");
    }
}
