package br.com.tresvtintas.mobile.core.appointment;

@FunctionalInterface
public interface AppointmentListStateListener {
    void onAppointmentListStateChanged(AppointmentListState state);
}
