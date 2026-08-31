package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/** Resolves a tracking scope only from the latest server-issued authorization snapshot. */
final class WorkforceRuntimeScopeResolver {
    private WorkforceRuntimeScopeResolver() {
        throw new AssertionError("No instances.");
    }

    static OptionalLong resolve(BootstrapSnapshot bootstrap) {
        Objects.requireNonNull(bootstrap, "Bootstrap snapshot is required.");
        var authorization = bootstrap.authorization();
        if (!authorization.has(Capability.LOCATION_TRACK_SELF)
                || authorization.organizationAccessMode()
                        != OrganizationAccessMode.ASSIGNED
                || authorization.organizationPageHasMore()) {
            return OptionalLong.empty();
        }
        List<OrganizationScope> organizations = authorization.organizations();
        if (authorization.defaultOrganizationId().isPresent()) {
            long defaultId = authorization.defaultOrganizationId().getAsLong();
            return organizations.stream()
                    .filter(organization -> organization.id() == defaultId)
                    .findFirst()
                    .map(organization -> OptionalLong.of(organization.id()))
                    .orElseGet(OptionalLong::empty);
        }
        return organizations.size() == 1
                ? OptionalLong.of(organizations.get(0).id())
                : OptionalLong.empty();
    }
}
