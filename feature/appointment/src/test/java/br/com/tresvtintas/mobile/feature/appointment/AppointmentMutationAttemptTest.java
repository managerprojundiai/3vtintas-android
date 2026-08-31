package br.com.tresvtintas.mobile.feature.appointment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationAction;
import org.junit.Test;

public final class AppointmentMutationAttemptTest {
    @Test
    public void reusesKeyOnlyForTheSameLogicalMutation() {
        AppointmentMutationAttempt attempt =
                new AppointmentMutationAttempt();

        String first = attempt.keyFor(
                AppointmentMutationAction.UPDATE,
                701,
                4,
                "Visita\n2026-08-01T13:00:00Z");
        String retry = attempt.keyFor(
                AppointmentMutationAction.UPDATE,
                701,
                4,
                "Visita\n2026-08-01T13:00:00Z");
        String changedPayload = attempt.keyFor(
                AppointmentMutationAction.UPDATE,
                701,
                4,
                "Visita\n2026-08-01T14:00:00Z");
        String changedRevision = attempt.keyFor(
                AppointmentMutationAction.UPDATE,
                701,
                5,
                "Visita\n2026-08-01T14:00:00Z");

        assertEquals(
                "An identical retry must preserve its idempotency key.",
                first,
                retry);
        assertNotEquals(
                "A changed payload must receive another key.",
                first,
                changedPayload);
        assertNotEquals(
                "A changed revision must receive another key.",
                changedPayload,
                changedRevision);
    }

    @Test
    public void invalidRestoredKeyIsNeverReused() {
        AppointmentMutationAttempt attempt =
                AppointmentMutationAttempt.restored("invalid", "stale");

        assertNotEquals(
                "An invalid restored key must never be reused.",
                "invalid",
                attempt.keyFor(
                        AppointmentMutationAction.TRANSITION,
                        701,
                        4,
                        "CONFIRMED"));
    }
}
