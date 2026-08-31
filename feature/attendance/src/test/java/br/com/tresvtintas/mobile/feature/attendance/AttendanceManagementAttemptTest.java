package br.com.tresvtintas.mobile.feature.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import java.util.OptionalLong;
import org.junit.Test;

public final class AttendanceManagementAttemptTest {
    private static final String CONVERSATION_ID = "whatsapp:1";

    @Test
    public void sameMutationReusesTheSameKey() {
        AttendanceManagementAttempt attempt =
                new AttendanceManagementAttempt();
        AttendanceManagementSelection selection = selection();

        String first = attempt.keyFor(CONVERSATION_ID, 3, selection);
        String second = attempt.keyFor(CONVERSATION_ID, 3, selection);

        assertEquals(
                "A retry of the same mutation must reuse its key.",
                first,
                second);
        assertEquals(
                "The fingerprint must not contain the selected values.",
                64,
                attempt.fingerprint().length());
        assertFalse(
                "Only the digest may enter saved state.",
                attempt.fingerprint().contains("resolved"));
    }

    @Test
    public void changedRevisionOrSelectionCreatesANewKey() {
        AttendanceManagementAttempt attempt =
                new AttendanceManagementAttempt();
        String first = attempt.keyFor(
                CONVERSATION_ID,
                3,
                selection());
        String second = attempt.keyFor(
                CONVERSATION_ID,
                4,
                selection());
        String third = attempt.keyFor(
                CONVERSATION_ID,
                4,
                new AttendanceManagementSelection(
                        AttendanceFolder.INBOX,
                        AttendancePriority.URGENT,
                        OptionalLong.of(22)));

        assertNotEquals(
                "A new revision must receive a new key.",
                first,
                second);
        assertNotEquals(
                "A different selection must receive a new key.",
                second,
                third);
    }

    @Test
    public void restorationAcceptsOnlyValidatedOpaqueState() {
        AttendanceManagementAttempt original =
                new AttendanceManagementAttempt();
        original.keyFor(CONVERSATION_ID, 3, selection());

        AttendanceManagementAttempt restored =
                AttendanceManagementAttempt.restored(
                        original.key(),
                        original.fingerprint());
        AttendanceManagementAttempt rejected =
                AttendanceManagementAttempt.restored(
                        "invalid",
                        "stale");

        assertEquals(
                "Validated saved state must be restored.",
                original.key(),
                restored.key());
        assertEquals(
                "Malformed keys must be discarded.",
                "",
                rejected.key());
        assertEquals(
                "Malformed fingerprints must be discarded.",
                "",
                rejected.fingerprint());
    }

    private static AttendanceManagementSelection selection() {
        return new AttendanceManagementSelection(
                AttendanceFolder.RESOLVED,
                AttendancePriority.HIGH,
                OptionalLong.empty());
    }
}
