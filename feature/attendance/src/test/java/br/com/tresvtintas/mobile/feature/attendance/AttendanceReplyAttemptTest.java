package br.com.tresvtintas.mobile.feature.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AttendanceReplyAttemptTest {
    private static final String CONVERSATION_ID = "whatsapp:1";
    private static final String REPLY = "Resposta";
    private static final String SENSITIVE_CONTENT = "Conteúdo sensível";

    @Test
    public void reusesKeyOnlyForTheSameConversationAndContent() {
        AttendanceReplyAttempt attempt = new AttendanceReplyAttempt();

        String first = attempt.keyFor(
                CONVERSATION_ID,
                REPLY);
        String retry = attempt.keyFor(
                CONVERSATION_ID,
                REPLY);
        String edited = attempt.keyFor(
                CONVERSATION_ID,
                "Resposta editada");
        String anotherConversation = attempt.keyFor(
                "whatsapp:2",
                "Resposta editada");

        assertEquals(
                "A retry of the same logical message must reuse its key.",
                first,
                retry);
        assertNotEquals(
                "Editing content must create another logical attempt.",
                first,
                edited);
        assertNotEquals(
                "Conversation identity belongs to the fingerprint.",
                edited,
                anotherConversation);
    }

    @Test
    public void restoresOnlyOpaqueKeyAndDigest() {
        AttendanceReplyAttempt attempt = new AttendanceReplyAttempt();
        String key = attempt.keyFor(
                "site_chat:session-1",
                SENSITIVE_CONTENT);

        AttendanceReplyAttempt restored =
                AttendanceReplyAttempt.restored(
                        attempt.key(),
                        attempt.fingerprint());

        assertEquals(
                "Configuration recreation must preserve a safe retry.",
                key,
                restored.keyFor(
                        "site_chat:session-1",
                        SENSITIVE_CONTENT));
        assertEquals(
                "Only a fixed-size SHA-256 digest may be retained.",
                64,
                restored.fingerprint().length());
        assertTrue(
                "The persisted state must not contain message content.",
                !restored.fingerprint().contains("Conteúdo")
                        && !restored.key().contains("Conteúdo"));
    }

    @Test
    public void malformedRestoredStateFailsClosed() {
        AttendanceReplyAttempt restored =
                AttendanceReplyAttempt.restored(
                        "short",
                        SENSITIVE_CONTENT);

        assertNotEquals(
                "Malformed state must not be trusted.",
                "short",
                restored.keyFor(
                        CONVERSATION_ID,
                        SENSITIVE_CONTENT));
    }

    @Test
    public void resetStartsANewLogicalAttempt() {
        AttendanceReplyAttempt attempt = new AttendanceReplyAttempt();
        String first = attempt.keyFor(CONVERSATION_ID, REPLY);
        attempt.reset();

        assertNotEquals(
                "A terminal success must start a fresh future attempt.",
                first,
                attempt.keyFor(CONVERSATION_ID, REPLY));
    }
}
