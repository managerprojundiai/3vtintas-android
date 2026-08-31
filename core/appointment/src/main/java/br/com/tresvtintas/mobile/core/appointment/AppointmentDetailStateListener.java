package br.com.tresvtintas.mobile.core.appointment;

@FunctionalInterface
public interface AppointmentDetailStateListener {
    void onAppointmentDetailStateChanged(AppointmentDetailState state);
}
