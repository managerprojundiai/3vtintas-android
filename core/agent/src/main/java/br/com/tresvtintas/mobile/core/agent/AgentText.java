package br.com.tresvtintas.mobile.core.agent;

public final class AgentText {
    public static final int MAXIMUM_MESSAGE_CHARS = 4_000;
    public static final int MAXIMUM_TITLE_CHARS = 120;

    private AgentText() {
        throw new AssertionError("No instances.");
    }

    public static String message(String value) {
        String normalized = String.valueOf(value == null ? "" : value)
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .trim();
        if (normalized.isEmpty()
                || normalized.length() > MAXIMUM_MESSAGE_CHARS) {
            throw new IllegalArgumentException(
                    "Agent message is invalid.");
        }
        return normalized;
    }

    public static String title(String value) {
        String normalized = String.valueOf(value == null ? "" : value)
                .replace("\u0000", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()
                || normalized.length() > MAXIMUM_TITLE_CHARS) {
            throw new IllegalArgumentException(
                    "Agent conversation title is invalid.");
        }
        return normalized;
    }
}
