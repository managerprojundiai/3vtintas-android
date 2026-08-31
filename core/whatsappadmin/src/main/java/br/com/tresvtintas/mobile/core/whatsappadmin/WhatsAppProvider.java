package br.com.tresvtintas.mobile.core.whatsappadmin;

public enum WhatsAppProvider {
    META_CLOUD("meta_cloud"),
    EVOLUTION("evolution");

    private final String wireValue;

    WhatsAppProvider(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static WhatsAppProvider fromWireValue(String value) {
        for (WhatsAppProvider candidate : values()) {
            if (candidate.wireValue.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("WhatsApp provider is invalid.");
    }
}
