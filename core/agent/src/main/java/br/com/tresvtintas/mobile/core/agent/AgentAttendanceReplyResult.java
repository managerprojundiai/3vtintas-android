package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.regex.Pattern;

public record AgentAttendanceReplyResult(
        String conversationId,
        AgentAttendanceDeliveryState deliveryState,
        String messageId) implements AgentActionResult {
    private static final Pattern CONVERSATION_ID = Pattern.compile(
            "^(?:whatsapp:[1-9]\\d*|site_chat:[^/\\u0000-\\u001f\\u007f]{1,160})$");

    public AgentAttendanceReplyResult {
        if (conversationId == null
                || !CONVERSATION_ID.matcher(conversationId).matches()) {
            throw new IllegalArgumentException(
                    "Attendance conversation ID is invalid.");
        }
        deliveryState = Objects.requireNonNull(
                deliveryState,
                "Attendance delivery state is required.");
        if (messageId == null
                || messageId.isBlank()
                || messageId.length() > 400) {
            throw new IllegalArgumentException(
                    "Attendance message ID is invalid.");
        }
    }
}
