package br.com.tresvtintas.mobile.data.audit;

import br.com.tresvtintas.mobile.core.audit.AuditModels;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Actor;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.network.dto.AuditPageDto;
import java.time.Instant;
import java.util.Optional;

final class AuditDtoMapper {
    private AuditDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Page page(AuditPageDto value) {
        return AuditModels.page(
                value.items().stream().map(AuditDtoMapper::event).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    private static Event event(AuditPageDto.Event value) {
        return new Event(
                value.action(),
                Optional.ofNullable(value.entity()),
                Optional.ofNullable(value.actor()).map(AuditDtoMapper::actor),
                Instant.parse(value.occurredAt()));
    }

    private static Actor actor(AuditPageDto.Actor value) {
        AppRole role = AppRole.fromWireValue(value.role()).orElseThrow(
                () -> new IllegalArgumentException("Audit actor role is unknown."));
        return new Actor(value.name(), role);
    }
}
