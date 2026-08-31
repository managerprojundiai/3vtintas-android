package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class UserAdministrationDtosTest {
    private static final String MANAGER_ROLE = "manager";

    @Test
    public void validatesRoleFamiliesAndConfirmedCommands() {
        UserAdministrationDtos.StandardRoleRequest standard =
                new UserAdministrationDtos.StandardRoleRequest(
                        "standard_role",
                        4,
                        MANAGER_ROLE,
                        true);
        UserAdministrationDtos.OperationalRoleRequest operational =
                new UserAdministrationDtos.OperationalRoleRequest(
                        "operational_role",
                        4,
                        "salesperson",
                        7,
                        true);

        assertEquals(
                "The standard command role must be retained.",
                MANAGER_ROLE,
                standard.role());
        assertEquals(
                "The operational organization must be retained.",
                7,
                operational.organizationId());
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserAdministrationDtos.StandardRoleRequest(
                        "standard_role",
                        4,
                        "salesperson",
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserAdministrationDtos.AccountStatusRequest(
                        "account_status",
                        4,
                        true,
                        false));
    }

    @Test
    public void acceptsOnlyKnownServerRolesAndValidRevisions() {
        assertTrue(
                "Master admin must be recognized in server responses.",
                UserAdministrationDtos.supportsRole("master_admin"));
        assertFalse(
                "Unknown authority labels must fail closed.",
                UserAdministrationDtos.supportsRole("super_user"));
        assertThrows(
                IllegalArgumentException.class,
                () -> user(0));
    }

    @Test
    public void optionCollectionsAreImmutableSnapshots() {
        List<UserAdministrationDtos.Organization> organizations =
                new ArrayList<>();
        organizations.add(new UserAdministrationDtos.Organization(
                7,
                "Jundiaí",
                "jundiai"));
        UserAdministrationDtos.Options options =
                new UserAdministrationDtos.Options(
                        organizations,
                        List.of(MANAGER_ROLE, "painter"),
                        List.of("salesperson", "delivery_driver"));

        organizations.clear();

        assertEquals(
                "DTO options must retain their original snapshot.",
                1,
                options.organizations().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> options.organizations().clear());
    }

    private static UserAdministrationDtos.User user(int revision) {
        return new UserAdministrationDtos.User(
                31,
                "Maria",
                "maria@example.com",
                MANAGER_ROLE,
                false,
                revision,
                List.of(),
                "2026-07-30T11:00:00Z",
                "2026-07-30T12:00:00Z");
    }
}
