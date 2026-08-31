package br.com.tresvtintas.mobile.data.painteradmin;

import br.com.tresvtintas.mobile.core.network.dto.PainterAdministrationDtos;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

final class PainterAdministrationDtoMapper {
    private PainterAdministrationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Options options(PainterAdministrationDtos.Options value) {
        return new Options(
                value.organizations().stream()
                        .map(PainterAdministrationDtoMapper::organization)
                        .toList(),
                value.managers().stream()
                        .map(PainterAdministrationDtoMapper::manager)
                        .toList());
    }

    static Page<Painter> painters(
            PainterAdministrationDtos.PainterPage value) {
        return new Page<>(
                value.items().stream()
                        .map(PainterAdministrationDtoMapper::painter)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static PainterDetail painterDetail(
            PainterAdministrationDtos.PainterDetail value) {
        return new PainterDetail(
                painter(new PainterAdministrationDtos.PainterSummary(
                        value.id(),
                        value.userId(),
                        value.name(),
                        value.email(),
                        value.company(),
                        value.specialty(),
                        value.status(),
                        value.commissionRate(),
                        value.organization(),
                        value.manager(),
                        value.revision(),
                        value.updatedAt())),
                optional(value.cpf()),
                optional(value.rg()),
                optional(value.phone()),
                optional(value.whatsappPhone()),
                optional(value.serviceArea()),
                optional(value.address()),
                optional(value.city()),
                optional(value.state()),
                optional(value.notes()));
    }

    static Page<AccessRequest> requests(
            PainterAdministrationDtos.AccessRequestPage value) {
        return new Page<>(
                value.items().stream()
                        .map(PainterAdministrationDtoMapper::accessRequest)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static AccessRequestDetail requestDetail(
            PainterAdministrationDtos.AccessRequestDetail value) {
        return new AccessRequestDetail(
                accessRequest(
                        new PainterAdministrationDtos.AccessRequestSummary(
                                value.id(),
                                value.userId(),
                                value.name(),
                                value.email(),
                                value.company(),
                                value.specialty(),
                                value.status(),
                                value.revision(),
                                value.createdAt(),
                                value.updatedAt())),
                optional(value.cpf()),
                value.phone(),
                optional(value.whatsappPhone()));
    }

    static Mutation mutation(
            PainterAdministrationDtos.Mutation value,
            boolean replayed) {
        return new Mutation(
                value.resourceId(),
                value.revision(),
                value.changed(),
                replayed);
    }

    private static Painter painter(
            PainterAdministrationDtos.PainterSummary value) {
        return new Painter(
                value.id(),
                value.userId(),
                value.name(),
                optional(value.email()),
                optional(value.company()),
                optional(value.specialty()),
                PainterAdministrationStatus.fromWireValue(value.status())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Painter status is unknown.")),
                new BigDecimal(value.commissionRate()),
                organization(value.organization()),
                Optional.ofNullable(value.manager())
                        .map(PainterAdministrationDtoMapper::manager),
                value.revision(),
                Instant.parse(value.updatedAt()));
    }

    private static AccessRequest accessRequest(
            PainterAdministrationDtos.AccessRequestSummary value) {
        return new AccessRequest(
                value.id(),
                value.userId(),
                value.name(),
                optional(value.email()),
                optional(value.company()),
                optional(value.specialty()),
                value.revision(),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static Organization organization(
            PainterAdministrationDtos.Organization value) {
        return new Organization(value.id(), value.name());
    }

    private static Manager manager(
            PainterAdministrationDtos.Manager value) {
        return new Manager(
                value.id(),
                value.name(),
                value.organizationId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.organizationId()));
    }

    private static Optional<String> optional(String value) {
        return Optional.ofNullable(value);
    }
}
