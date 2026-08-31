package br.com.tresvtintas.mobile.feature.appointment;

import java.util.Optional;

@FunctionalInterface
public interface AppointmentRuntimeProvider {
    Optional<AppointmentFeatureRuntime> appointmentRuntime();
}
