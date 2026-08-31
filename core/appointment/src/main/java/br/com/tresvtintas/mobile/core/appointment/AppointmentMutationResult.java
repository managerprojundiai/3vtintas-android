package br.com.tresvtintas.mobile.core.appointment;

public record AppointmentMutationResult(
        long appointmentId,
        AppointmentStatus status,
        long revision,
        boolean changed,
        boolean replayed) {
    public AppointmentMutationResult {
        if (appointmentId < 1 || status == null || revision < 1) {
            throw new IllegalArgumentException(
                    "Appointment mutation result is invalid.");
        }
    }
}
