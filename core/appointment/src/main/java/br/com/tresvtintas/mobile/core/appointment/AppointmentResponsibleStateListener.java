package br.com.tresvtintas.mobile.core.appointment;

@FunctionalInterface
public interface AppointmentResponsibleStateListener {
    void onAppointmentResponsibleStateChanged(
            AppointmentResponsibleState state);
}
