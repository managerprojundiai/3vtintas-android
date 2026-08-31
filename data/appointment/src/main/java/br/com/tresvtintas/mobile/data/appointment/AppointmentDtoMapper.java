package br.com.tresvtintas.mobile.data.appointment;

import br.com.tresvtintas.mobile.core.appointment.AppointmentAction;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentOverview;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPage;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPerson;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsiblePage;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.appointment.AppointmentSummary;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentPersonDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentResponsiblePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentSummaryDto;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class AppointmentDtoMapper {
    private AppointmentDtoMapper() {
    }

    static AppointmentPage page(AppointmentPageDto value) {
        return new AppointmentPage(
                value.items().stream()
                        .map(AppointmentDtoMapper::summary)
                        .toList(),
                new AppointmentOverview(
                        value.overview().scheduled(),
                        value.overview().confirmed(),
                        value.overview().completed(),
                        value.overview().cancelled()),
                Optional.ofNullable(value.nextCursor()));
    }

    static AppointmentResponsiblePage responsibles(
            AppointmentResponsiblePageDto value) {
        return new AppointmentResponsiblePage(
                value.items().stream()
                        .map(AppointmentDtoMapper::person)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static AppointmentDetail detail(AppointmentDetailDto value) {
        AppointmentSummaryDto summary = new AppointmentSummaryDto(
                value.id(),
                value.kind(),
                value.status(),
                value.title(),
                value.scheduledAt(),
                value.durationMinutes(),
                value.location(),
                value.responsible(),
                value.organization(),
                value.customer(),
                value.order(),
                value.revision(),
                value.allowedActions(),
                value.createdAt(),
                value.updatedAt());
        return new AppointmentDetail(
                summary(summary),
                Optional.ofNullable(value.description()));
    }

    private static AppointmentSummary summary(AppointmentSummaryDto value) {
        return new AppointmentSummary(
                value.id(),
                enumValue(AppointmentKind.class, value.kind()),
                enumValue(AppointmentStatus.class, value.status()),
                value.title(),
                Instant.parse(value.scheduledAt()),
                value.durationMinutes(),
                Optional.ofNullable(value.location()),
                person(value.responsible()),
                Optional.ofNullable(value.organization())
                        .map(item -> new AppointmentSummary.Organization(
                                item.id(),
                                item.name())),
                Optional.ofNullable(value.customer())
                        .map(item -> new AppointmentSummary.Customer(
                                item.id(),
                                item.name())),
                Optional.ofNullable(value.order())
                        .map(item -> new AppointmentSummary.Order(
                                item.id(),
                                item.type(),
                                item.status())),
                value.revision(),
                value.allowedActions().stream()
                        .map(item -> enumValue(
                                AppointmentAction.class,
                                item))
                        .collect(Collectors.toUnmodifiableSet()),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static AppointmentPerson person(AppointmentPersonDto value) {
        AppRole role = AppRole.fromWireValue(value.role())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment role is invalid."));
        return new AppointmentPerson(
                value.id(),
                Optional.ofNullable(value.name()),
                role);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
