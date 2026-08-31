package br.com.tresvtintas.mobile.feature.appointment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDraft;
import br.com.tresvtintas.mobile.core.appointment.AppointmentEdit;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPerson;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityFormBinding;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

final class AppointmentFormBinder {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final String STATE_DATE = "appointment_form_date";
    private static final String STATE_TIME = "appointment_form_time";
    private static final String STATE_KIND = "appointment_form_kind";
    private static final String STATE_RESPONSIBLE =
            "appointment_form_responsible";
    private static final String STATE_CUSTOMER =
            "appointment_form_customer";
    private final Context context;
    private final AppointmentActivityFormBinding binding;
    private List<AppointmentPerson> responsibleItems = List.of();
    private List<CustomerSummary> customerItems = List.of();
    private Optional<AppointmentPerson> selectedResponsible =
            Optional.empty();
    private OptionalLong selectedCustomerId = OptionalLong.empty();
    private OptionalLong pendingResponsibleId = OptionalLong.empty();
    private OptionalLong pendingCustomerId = OptionalLong.empty();
    private AppointmentKind kind = AppointmentKind.GENERAL;
    private LocalDate date;
    private LocalTime time;

    AppointmentFormBinder(
            Context context,
            AppointmentActivityFormBinding binding) {
        this.context = context;
        this.binding = binding;
        LocalDateTime nextHour = LocalDateTime.now()
                .plusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        date = nextHour.toLocalDate();
        time = nextHour.toLocalTime();
        configureKinds();
        binding.appointmentFormDurationInput.setText(
                R.string.appointment_default_duration);
        binding.appointmentFormDate.setOnClickListener(
                ignored -> chooseDate());
        binding.appointmentFormTime.setOnClickListener(
                ignored -> chooseTime());
        updateDateTime();
    }

    void configure(AppointmentFeatureRuntime runtime, boolean editing) {
        binding.appointmentFormResponsibleLayout.setVisibility(
                !editing && runtime.scope() != AppointmentScope.SELF
                        ? View.VISIBLE
                        : View.GONE);
        binding.appointmentFormCustomerLayout.setVisibility(
                runtime.customerRepository().isPresent()
                        ? View.VISIBLE
                        : View.GONE);
    }

    void restoreState(Bundle state) {
        try {
            LocalDate restoredDate = LocalDate.parse(
                    state.getString(STATE_DATE, date.toString()));
            LocalTime restoredTime = LocalTime.parse(
                    state.getString(STATE_TIME, time.toString()));
            AppointmentKind restoredKind = AppointmentKind.valueOf(
                    state.getString(STATE_KIND, kind.name()));
            date = restoredDate;
            time = restoredTime;
            kind = restoredKind;
            if (state.containsKey(STATE_RESPONSIBLE)) {
                pendingResponsibleId = OptionalLong.of(
                        state.getLong(STATE_RESPONSIBLE));
            }
            if (state.containsKey(STATE_CUSTOMER)) {
                long customerId = state.getLong(STATE_CUSTOMER);
                pendingCustomerId = OptionalLong.of(customerId);
                selectedCustomerId = OptionalLong.of(customerId);
            } else {
                pendingCustomerId = OptionalLong.empty();
                selectedCustomerId = OptionalLong.empty();
            }
            binding.appointmentFormKindInput.setText(
                    AppointmentText.kind(context, kind),
                    false);
            updateDateTime();
        } catch (IllegalArgumentException ignored) {
            // Invalid framework state fails safely to the initialized defaults.
        }
    }

    void saveState(Bundle state) {
        state.putString(STATE_DATE, date.toString());
        state.putString(STATE_TIME, time.toString());
        state.putString(STATE_KIND, kind.name());
        if (pendingResponsibleId.isPresent()) {
            state.putLong(
                    STATE_RESPONSIBLE,
                    pendingResponsibleId.orElseThrow());
        }
        if (selectedCustomerId.isPresent()) {
            state.putLong(
                    STATE_CUSTOMER,
                    selectedCustomerId.orElseThrow());
        }
    }

    void bind(AppointmentDetail detail) {
        var summary = detail.summary();
        binding.appointmentFormTitleInput.setText(summary.title());
        binding.appointmentFormDescriptionInput.setText(
                detail.description().orElse(""));
        binding.appointmentFormLocationInput.setText(
                summary.location().orElse(""));
        binding.appointmentFormDurationInput.setText(
                context.getString(
                        R.string.appointment_number,
                        summary.durationMinutes()));
        kind = summary.kind();
        binding.appointmentFormKindInput.setText(
                AppointmentText.kind(context, kind),
                false);
        LocalDateTime local = LocalDateTime.ofInstant(
                summary.scheduledAt(),
                ZoneId.systemDefault());
        date = local.toLocalDate();
        time = local.toLocalTime().withSecond(0).withNano(0);
        pendingResponsibleId = OptionalLong.of(summary.responsible().id());
        summary.customer().ifPresentOrElse(
                value -> {
                    pendingCustomerId = OptionalLong.of(value.id());
                    selectedCustomerId = OptionalLong.of(value.id());
                },
                () -> {
                    pendingCustomerId = OptionalLong.empty();
                    selectedCustomerId = OptionalLong.empty();
                });
        selectPending();
        updateDateTime();
    }

    void responsibles(List<AppointmentPerson> values) {
        responsibleItems = List.copyOf(values);
        List<String> labels = values.stream()
                .map(this::responsibleLabel)
                .collect(Collectors.toList());
        binding.appointmentFormResponsibleInput.setAdapter(
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        labels));
        binding.appointmentFormResponsibleInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedResponsible = Optional.of(
                            responsibleItems.get(position));
                    pendingResponsibleId = OptionalLong.of(
                            responsibleItems.get(position).id());
                });
        selectPending();
    }

    void customers(List<CustomerSummary> values) {
        customerItems = List.copyOf(values);
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(
                R.string.appointment_form_no_customer));
        values.stream().map(CustomerSummary::name).forEach(labels::add);
        binding.appointmentFormCustomerInput.setAdapter(
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        labels));
        binding.appointmentFormCustomerInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position == 0) {
                        selectedCustomerId = OptionalLong.empty();
                        pendingCustomerId = OptionalLong.empty();
                    } else {
                        long customerId = customerItems.get(position - 1).id();
                        selectedCustomerId = OptionalLong.of(customerId);
                        pendingCustomerId = OptionalLong.of(customerId);
                    }
                });
        selectPending();
    }

    AppointmentDraft draft(AppointmentFeatureRuntime runtime) {
        OptionalLong responsible = runtime.scope() == AppointmentScope.SELF
                ? OptionalLong.empty()
                : selectedResponsible
                        .map(value -> OptionalLong.of(value.id()))
                        .orElseGet(OptionalLong::empty);
        validateTarget(runtime, responsible);
        return new AppointmentDraft(
                runtime.scope(),
                responsible,
                runtime.organizationId(),
                kind,
                title(),
                optional(binding.appointmentFormDescriptionInput.getText()),
                instant(),
                duration(),
                optional(binding.appointmentFormLocationInput.getText()),
                selectedCustomerId);
    }

    AppointmentEdit edit(
            AppointmentFeatureRuntime runtime,
            long revision) {
        return new AppointmentEdit(
                runtime.scope(),
                revision,
                kind,
                title(),
                optional(binding.appointmentFormDescriptionInput.getText()),
                instant(),
                duration(),
                optional(binding.appointmentFormLocationInput.getText()),
                selectedCustomerId);
    }

    String fingerprint(AppointmentDraft draft) {
        return fields(
                draft.scope().name(),
                optionalLong(draft.responsibleUserId()),
                optionalLong(draft.organizationId()),
                draft.kind().name(),
                draft.title(),
                draft.description().orElse(""),
                draft.scheduledAt().toString(),
                Integer.toString(draft.durationMinutes()),
                draft.location().orElse(""),
                optionalLong(draft.customerId()));
    }

    String fingerprint(AppointmentEdit edit) {
        return fields(
                edit.scope().name(),
                Long.toString(edit.expectedRevision()),
                edit.kind().name(),
                edit.title(),
                edit.description().orElse(""),
                edit.scheduledAt().toString(),
                Integer.toString(edit.durationMinutes()),
                edit.location().orElse(""),
                optionalLong(edit.customerId()));
    }

    private void validateTarget(
            AppointmentFeatureRuntime runtime,
            OptionalLong responsible) {
        if (runtime.scope() != AppointmentScope.SELF
                && responsible.isEmpty()) {
            throw new FormException(
                    R.string.appointment_form_responsible_required);
        }
        boolean organizationRequired =
                runtime.scope() == AppointmentScope.TEAM
                        || selectedResponsible
                                .map(AppointmentPerson::role)
                                .filter(role ->
                                        role != AppRole.PAINTER)
                                .isPresent();
        if (organizationRequired && runtime.organizationId().isEmpty()) {
            throw new FormException(
                    R.string.appointment_form_organization_required);
        }
    }

    private String title() {
        String value = text(binding.appointmentFormTitleInput.getText());
        if (value.isBlank()) {
            binding.appointmentFormTitleLayout.setError(
                    context.getString(
                            R.string.appointment_form_required));
            throw new FormException(R.string.appointment_form_required);
        }
        binding.appointmentFormTitleLayout.setError(null);
        return value;
    }

    private int duration() {
        try {
            int value = Integer.parseInt(
                    text(binding.appointmentFormDurationInput.getText()));
            if (value < 1 || value > 1_440) {
                throw new NumberFormatException("Out of range.");
            }
            binding.appointmentFormDurationLayout.setError(null);
            return value;
        } catch (NumberFormatException exception) {
            binding.appointmentFormDurationLayout.setError(
                    context.getString(
                            R.string.appointment_form_required));
            throw new FormException(
                    R.string.appointment_form_required,
                    exception);
        }
    }

    private Instant instant() {
        return LocalDateTime.of(date, time)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    private void configureKinds() {
        List<AppointmentKind> kinds = List.of(
                AppointmentKind.GENERAL,
                AppointmentKind.COLLECTION);
        List<String> labels = kinds.stream()
                .map(value -> AppointmentText.kind(context, value))
                .collect(Collectors.toList());
        binding.appointmentFormKindInput.setAdapter(
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        labels));
        binding.appointmentFormKindInput.setText(labels.get(0), false);
        binding.appointmentFormKindInput.setOnItemClickListener(
                (parent, view, position, id) -> kind = kinds.get(position));
    }

    private void chooseDate() {
        new DatePickerDialog(
                context,
                (view, year, month, day) -> {
                    date = LocalDate.of(year, month + 1, day);
                    updateDateTime();
                },
                date.getYear(),
                date.getMonthValue() - 1,
                date.getDayOfMonth())
                .show();
    }

    private void chooseTime() {
        new TimePickerDialog(
                context,
                (view, hour, minute) -> {
                    time = LocalTime.of(hour, minute);
                    updateDateTime();
                },
                time.getHour(),
                time.getMinute(),
                true)
                .show();
    }

    private void updateDateTime() {
        binding.appointmentFormDate.setText(DATE.format(date));
        binding.appointmentFormTime.setText(TIME.format(time));
    }

    private void selectPending() {
        if (pendingResponsibleId.isPresent()) {
            for (AppointmentPerson value : responsibleItems) {
                if (value.id() == pendingResponsibleId.orElseThrow()) {
                    selectedResponsible = Optional.of(value);
                    binding.appointmentFormResponsibleInput.setText(
                            responsibleLabel(value),
                            false);
                    break;
                }
            }
        }
        if (pendingCustomerId.isPresent()) {
            for (CustomerSummary value : customerItems) {
                if (value.id() == pendingCustomerId.orElseThrow()) {
                    selectedCustomerId = OptionalLong.of(value.id());
                    binding.appointmentFormCustomerInput.setText(
                            value.name(),
                            false);
                    break;
                }
            }
        } else if (!customerItems.isEmpty()) {
            binding.appointmentFormCustomerInput.setText(
                    R.string.appointment_form_no_customer);
        }
    }

    private String responsibleLabel(AppointmentPerson value) {
        return context.getString(
                R.string.appointment_responsible_label,
                AppointmentText.person(context, value),
                AppointmentText.role(context, value.role()));
    }

    private static Optional<String> optional(
            android.text.Editable value) {
        String normalized = text(value);
        return normalized.isBlank()
                ? Optional.empty()
                : Optional.of(normalized);
    }

    private static String text(android.text.Editable value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String optionalLong(OptionalLong value) {
        return value != null && value.isPresent()
                ? Long.toString(value.orElseThrow())
                : "";
    }

    private static String fields(String... values) {
        return String.join("\n", values);
    }

    static final class FormException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final int messageResource;

        FormException(int messageResource) {
            super("Appointment form is invalid.");
            this.messageResource = messageResource;
        }

        FormException(int messageResource, Throwable cause) {
            super("Appointment form is invalid.", cause);
            this.messageResource = messageResource;
        }

        int messageResource() {
            return messageResource;
        }
    }
}
