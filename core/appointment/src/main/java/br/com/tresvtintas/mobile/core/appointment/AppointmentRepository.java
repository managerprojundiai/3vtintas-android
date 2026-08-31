package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public interface AppointmentRepository {
    AppointmentPage page(
            AppointmentQuery query,
            Optional<String> cursor) throws AppointmentException;

    AppointmentDetail detail(
            long appointmentId,
            AppointmentScope scope) throws AppointmentException;

    AppointmentResponsiblePage responsibles(
            AppointmentResponsibleQuery query,
            Optional<String> cursor) throws AppointmentException;

    AppointmentMutationResult create(
            AppointmentDraft draft,
            String idempotencyKey) throws AppointmentException;

    AppointmentMutationResult update(
            long appointmentId,
            AppointmentEdit edit,
            String idempotencyKey) throws AppointmentException;

    AppointmentMutationResult transition(
            long appointmentId,
            AppointmentScope scope,
            long expectedRevision,
            AppointmentStatus status,
            String idempotencyKey) throws AppointmentException;
}
