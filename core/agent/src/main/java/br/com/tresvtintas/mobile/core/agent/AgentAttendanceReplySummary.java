package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record AgentAttendanceReplySummary(
        String conversationId,
        AgentAttendanceChannel channel,
        String customerName,
        Optional<String> organizationName,
        int expectedRevision,
        String content,
        Optional<AgentAttendanceLatestMessage> latestMessage,
        boolean accessAndRevisionWillBeRevalidated)
        implements AgentActionSummary {
    private static final int MINIMUM_REVISION = 1;
    private static final Pattern CONVERSATION_ID = Pattern.compile(
            "^(?:whatsapp:[1-9]\\d*|site_chat:[^/\\u0000-\\u001f\\u007f]{1,160})$");

    public AgentAttendanceReplySummary {
        if (conversationId == null
                || !CONVERSATION_ID.matcher(conversationId).matches()) {
            throw new IllegalArgumentException(
                    "Attendance conversation ID is invalid.");
        }
        channel = Objects.requireNonNull(
                channel,
                "Attendance channel is required.");
        customerName = requireText(
                customerName,
                500,
                "Attendance customer is invalid.");
        organizationName = Objects.requireNonNull(
                organizationName,
                "Attendance organization is required.");
        organizationName = organizationName.map(value ->
                requireText(
                        value,
                        200,
                        "Attendance organization is invalid."));
        if (expectedRevision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Attendance revision is invalid.");
        }
        content = requireText(
                content,
                3_500,
                "Attendance reply is invalid.");
        latestMessage = Objects.requireNonNull(
                latestMessage,
                "Attendance latest message is required.");
        if (!accessAndRevisionWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Attendance revalidation is required.");
        }
    }

    private static String requireText(
            String value,
            int maximum,
            String message) {
        if (value == null
                || value.isBlank()
                || value.length() > maximum
                || value.matches(
                        ".*[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\u007f].*")) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
