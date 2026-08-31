package br.com.tresvtintas.mobile.core.agent;

public enum AgentDocumentType {
    MATERIAL_QUOTE_PDF("material_quote_pdf", "material"),
    LABOR_QUOTE_PDF("labor_quote_pdf", "labor");

    private final String wireValue;
    private final String filenameSuffix;

    AgentDocumentType(
            String wireValue,
            String filenameSuffix) {
        this.wireValue = wireValue;
        this.filenameSuffix = filenameSuffix;
    }

    public String wireValue() {
        return wireValue;
    }

    String filenameSuffix() {
        return filenameSuffix;
    }

    public static AgentDocumentType fromWireValue(String value) {
        for (AgentDocumentType type : values()) {
            if (type.wireValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Agent document type is invalid.");
    }
}
