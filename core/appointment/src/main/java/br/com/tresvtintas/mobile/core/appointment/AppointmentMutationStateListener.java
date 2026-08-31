package br.com.tresvtintas.mobile.core.appointment;

@FunctionalInterface
public interface AppointmentMutationStateListener {
    void onAppointmentMutationStateChanged(AppointmentMutationState state);
}
