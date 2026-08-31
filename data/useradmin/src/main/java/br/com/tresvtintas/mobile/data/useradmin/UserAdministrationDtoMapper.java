package br.com.tresvtintas.mobile.data.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.network.dto.UserAdministrationDtos;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Assignment;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.time.Instant;
import java.util.Optional;

final class UserAdministrationDtoMapper {
    private UserAdministrationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Options options(UserAdministrationDtos.Options value) {
        return new Options(
                value.organizations().stream()
                        .map(UserAdministrationDtoMapper::organization)
                        .toList(),
                value.standardRoles().stream().map(UserAdministrationDtoMapper::role).toList(),
                value.operationalRoles().stream()
                        .map(UserAdministrationDtoMapper::role)
                        .toList());
    }

    static Page page(UserAdministrationDtos.Page value) {
        return new Page(
                value.items().stream().map(UserAdministrationDtoMapper::user).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static User user(UserAdministrationDtos.User value) {
        return new User(
                value.id(),
                value.name(),
                Optional.ofNullable(value.email()),
                role(value.role()),
                value.isBlocked(),
                value.revision(),
                value.assignments().stream()
                        .map(UserAdministrationDtoMapper::assignment)
                        .toList(),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    static Mutation mutation(UserAdministrationDtos.Mutation value, boolean replayed) {
        return new Mutation(
                value.resourceId(),
                value.revision(),
                value.changed(),
                replayed);
    }

    private static Organization organization(UserAdministrationDtos.Organization value) {
        return new Organization(value.id(), value.name(), value.slug());
    }

    private static Assignment assignment(UserAdministrationDtos.Assignment value) {
        return new Assignment(
                value.organizationId(),
                value.organizationName(),
                role(value.role()),
                Optional.ofNullable(value.approvedAt()).map(Instant::parse));
    }

    private static AppRole role(String value) {
        return AppRole.fromWireValue(value).orElseThrow(
                () -> new IllegalArgumentException("User role is unknown."));
    }
}
