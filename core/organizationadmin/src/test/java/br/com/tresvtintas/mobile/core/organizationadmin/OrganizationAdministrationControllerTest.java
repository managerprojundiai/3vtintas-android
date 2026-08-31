package br.com.tresvtintas.mobile.core.organizationadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Page;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public final class OrganizationAdministrationControllerTest {
    @Test
    public void controllerPublishesAuthorizedPage() {
        AtomicReference<OrganizationAdministrationListState> observed =
                new AtomicReference<>();
        OrganizationAdministrationListController controller =
                new OrganizationAdministrationListController(
                        repository(),
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(observed::set);

        controller.open();

        assertEquals(
                "The list controller must publish its authorized result",
                OrganizationAdministrationListState.Phase.READY,
                observed.get().phase());
        assertEquals(
                "The authorized organization must be exposed to the screen",
                "3V Jundiaí",
                observed.get().snapshot().orElseThrow()
                        .organizations().get(0).name());
    }

    @Test
    public void taskControllerPublishesFailureWithoutThrowingOnMainThread() {
        AtomicReference<OrganizationAdministrationTaskState<Mutation>> observed =
                new AtomicReference<>();
        OrganizationAdministrationTaskController<Mutation> controller =
                new OrganizationAdministrationTaskController<>(
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(observed::set);

        controller.submit(() -> {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.CONFLICT,
                    "conflict");
        });

        assertEquals(
                "Task failures must become an explicit UI state",
                OrganizationAdministrationTaskState.Phase.ERROR,
                observed.get().phase());
        assertTrue(
                "The error state must preserve the classified failure",
                observed.get().failure().isPresent());
    }

    private static OrganizationAdministrationRepository repository() {
        return new OrganizationAdministrationRepository() {
            @Override
            public Page organizations(
                    OrganizationAdministrationQuery query,
                    Optional<String> cursor) {
                return new Page(
                        List.of(OrganizationAdministrationControllerTest.organization()),
                        Optional.empty());
            }

            @Override
            public Organization organization(long organizationId) {
                return OrganizationAdministrationControllerTest.organization();
            }

            @Override
            public Mutation create(String name, String slug, String key) {
                return mutation();
            }

            @Override
            public Mutation rename(
                    long organizationId,
                    int revision,
                    String name,
                    String key) {
                return mutation();
            }

            @Override
            public Mutation setStatus(
                    long organizationId,
                    int revision,
                    OrganizationAdministrationStatus status,
                    String key) {
                return mutation();
            }
        };
    }

    private static Organization organization() {
        Instant now = Instant.parse("2026-07-30T12:00:00Z");
        return new Organization(
                1,
                "3v-jundiai",
                "3V Jundiaí",
                OrganizationAdministrationStatus.ACTIVE,
                1,
                0,
                0,
                0,
                0,
                now,
                now);
    }

    private static Mutation mutation() {
        return new Mutation(1, 1, false, false);
    }
}
