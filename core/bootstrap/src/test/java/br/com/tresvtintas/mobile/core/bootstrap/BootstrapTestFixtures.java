package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapApiStatus;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapAuthorization;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationAccess;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationScopeDto;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.util.List;

final class BootstrapTestFixtures {
    static final long USER_ID = 41;
    static final String SESSION_ID = "00000000-0000-4000-8000-000000000041";
    static final String DEVICE_ID = "00000000-0000-4000-8000-000000000042";
    static final String REVISION = "a".repeat(64);

    private BootstrapTestFixtures() {
        throw new AssertionError("No instances.");
    }

    static ExpectedBootstrapIdentity expected() {
        return new ExpectedBootstrapIdentity(USER_ID, SESSION_ID, DEVICE_ID);
    }

    static ClientCompatibility compatibility() {
        return new ClientCompatibility("v1", "0.5.0", 5, 35);
    }

    static BootstrapResponse response() {
        return response(
                new BootstrapApiStatus(
                        "v1",
                        "0.5.0",
                        "0.1.0",
                        2,
                        26,
                        false,
                        "2026-07-25T13:00:00.000Z"),
                List.of("catalog.read", "quote.create", "future.capability"));
    }

    static BootstrapResponse response(
            BootstrapApiStatus api,
            List<String> capabilities) {
        return new BootstrapResponse(
                new AuthenticatedUser(
                        USER_ID, "Pessoa", "pessoa@example.test", "salesperson"),
                new SessionIdentity(SESSION_ID, DEVICE_ID),
                new BootstrapAuthorization(
                        capabilities,
                        new OrganizationAccess(
                                "assigned",
                                List.of(new OrganizationScopeDto(
                                        9,
                                        "3V Centro",
                                        "3v-centro",
                                        "salesperson")),
                                false,
                                9L),
                        REVISION),
                api);
    }
}
