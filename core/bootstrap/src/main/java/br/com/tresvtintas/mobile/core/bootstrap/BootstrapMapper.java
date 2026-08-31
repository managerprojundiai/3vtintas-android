package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.model.OrganizationMembershipRole;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapAuthorization;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationAccess;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationScopeDto;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

final class BootstrapMapper {
    private final ClientCompatibility client;

    BootstrapMapper(ClientCompatibility client) {
        if (client == null) {
            throw new IllegalArgumentException("Client compatibility is required.");
        }
        this.client = client;
    }

    BootstrapSnapshot map(
            BootstrapResponse response,
            ExpectedBootstrapIdentity expected) throws BootstrapException {
        if (response == null || expected == null) {
            throw protocol("Bootstrap response and expected identity are required.", null);
        }
        verifySession(response, expected);
        verifyCompatibility(response);
        try {
            AppRole role = AppRole.fromWireValue(response.user().role())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown account role."));
            AuthorizationSnapshot authorization = mapAuthorization(response.authorization());
            return new BootstrapSnapshot(
                    response.user(),
                    response.session(),
                    role,
                    authorization,
                    response.api().apiVersion(),
                    response.api().contractVersion(),
                    Instant.parse(response.api().serverTime()));
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw protocol("The bootstrap response violated the mobile contract.", exception);
        }
    }

    private void verifySession(
            BootstrapResponse response,
            ExpectedBootstrapIdentity expected) throws BootstrapException {
        boolean matches = response.user().id() == expected.userId()
                && response.session().id().equals(expected.sessionId())
                && response.session().deviceId().equals(expected.deviceId());
        if (!matches) {
            throw protocol("Bootstrap identity did not match the authenticated session.", null);
        }
    }

    private void verifyCompatibility(BootstrapResponse response) throws BootstrapException {
        if (!client.apiVersion().equals(response.api().apiVersion())
                || !client.contractVersion().equals(response.api().contractVersion())) {
            throw protocol("The API contract is not supported by this application.", null);
        }
        if (response.api().minimumSupportedAppVersionCode() > client.appVersionCode()) {
            throw new BootstrapException(
                    BootstrapFailureKind.UPDATE_REQUIRED,
                    "This application version is no longer supported.");
        }
        if (response.api().minimumSupportedAndroidApiLevel() > client.androidApiLevel()) {
            throw new BootstrapException(
                    BootstrapFailureKind.DEVICE_UNSUPPORTED,
                    "This Android version is no longer supported.");
        }
        if (response.api().maintenance()) {
            throw new BootstrapException(
                    BootstrapFailureKind.MAINTENANCE,
                    "The mobile service is under maintenance.");
        }
    }

    private static AuthorizationSnapshot mapAuthorization(BootstrapAuthorization source) {
        Set<String> uniqueWireCapabilities = new HashSet<>();
        Set<Capability> known = EnumSet.noneOf(Capability.class);
        int ignored = 0;
        for (String value : source.capabilities()) {
            if (!uniqueWireCapabilities.add(value)) {
                throw new IllegalArgumentException("Capabilities must not be duplicated.");
            }
            Capability capability = Capability.fromWireValue(value).orElse(null);
            if (capability == null) {
                ignored++;
            } else {
                known.add(capability);
            }
        }
        OrganizationAccess access = source.organizationAccess();
        OrganizationAccessMode mode = OrganizationAccessMode.fromWireValue(access.mode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown organization access mode."));
        List<OrganizationScope> organizations = new ArrayList<>();
        for (OrganizationScopeDto organization : access.organizations()) {
            organizations.add(new OrganizationScope(
                    organization.id(),
                    organization.name(),
                    organization.slug(),
                    OrganizationMembershipRole.fromWireValue(organization.membershipRole())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Unknown organization membership role."))));
        }
        OptionalLong defaultId = access.defaultOrganizationId() == null
                ? OptionalLong.empty()
                : OptionalLong.of(access.defaultOrganizationId());
        return new AuthorizationSnapshot(
                known,
                mode,
                organizations,
                access.hasMore(),
                defaultId,
                source.revision(),
                ignored);
    }

    private static BootstrapException protocol(String message, Throwable cause) {
        return new BootstrapException(BootstrapFailureKind.PROTOCOL, message, cause);
    }
}
