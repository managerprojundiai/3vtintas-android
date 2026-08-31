package br.com.tresvtintas.mobile.core.useradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Assignment;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class UserAdministrationModelsTest {
    @Test
    public void separatesStandardAndOperationalRoles() {
        List<Organization> organizations = new ArrayList<>();
        organizations.add(new Organization(7, " Jundiaí ", " jundiai "));
        Options options = new Options(
                organizations,
                List.of(AppRole.MANAGER, AppRole.PAINTER),
                List.of(AppRole.SALESPERSON, AppRole.DELIVERY_DRIVER));

        organizations.clear();

        assertEquals(
                "Options must retain an immutable organization snapshot.",
                1,
                options.organizations().size());
        assertEquals(
                "Organization names must be normalized.",
                "Jundiaí",
                options.organizations().get(0).name());
        assertThrows(
                IllegalArgumentException.class,
                () -> new Options(
                        List.of(),
                        List.of(AppRole.SALESPERSON),
                        List.of(AppRole.DELIVERY_DRIVER)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Options(
                        List.of(),
                        List.of(AppRole.MANAGER),
                        List.of(AppRole.CUSTOMER)));
    }

    @Test
    public void protectsActorAndMasterAdministratorFromClientChanges() {
        User self = user(31, AppRole.MANAGER, false);
        User master = user(32, AppRole.MASTER_ADMIN, false);
        User colleague = user(33, AppRole.MANAGER, true);

        assertTrue(
                "The signed-in actor must be protected in the UI.",
                self.protectedFromChanges(31));
        assertTrue(
                "Master administrators must remain protected in the UI.",
                master.protectedFromChanges(31));
        assertFalse(
                "A different standard user may be managed.",
                colleague.protectedFromChanges(31));
        assertEquals(
                "Blocked state must map to the canonical status.",
                UserAdministrationStatus.BLOCKED,
                colleague.status());
    }

    @Test
    public void validatesAssignmentsAndNormalizesQueries() {
        Assignment assignment = new Assignment(
                7,
                " Jundiaí ",
                AppRole.SALESPERSON,
                Optional.empty());
        UserAdministrationQuery query = UserAdministrationQuery.initial()
                .withOrganization(OptionalLong.of(7))
                .withRole(Optional.of(AppRole.SALESPERSON))
                .withStatus(Optional.of(UserAdministrationStatus.ACTIVE))
                .withSearch("  Maria  ");

        assertEquals(
                "Assignment organization names must be normalized.",
                "Jundiaí",
                assignment.organizationName());
        assertEquals(
                "Search terms must be normalized.",
                Optional.of("Maria"),
                query.search());
        assertThrows(
                IllegalArgumentException.class,
                () -> new Assignment(
                        7,
                        "Jundiaí",
                        AppRole.MANAGER,
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserAdministrationQuery(
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("x".repeat(81)),
                        30));
    }

    private static User user(long id, AppRole role, boolean blocked) {
        Instant created = Instant.parse("2026-07-30T11:00:00Z");
        return new User(
                id,
                "Usuário " + id,
                Optional.of("user@example.com"),
                role,
                blocked,
                4,
                List.of(),
                created,
                created.plusSeconds(60));
    }
}
