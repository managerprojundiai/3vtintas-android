package br.com.tresvtintas.mobile.core.commission;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class CommissionDetailControllerTest {
    @Test
    public void failsClosedBeforeRepositoryForInvalidIdentity() {
        FakeRepository repository = new FakeRepository();
        CommissionDetailController controller = new CommissionDetailController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.load(0, CommissionScope.SELF);

        CommissionDetailState state = state(controller);
        assertEquals(
                "Invalid identities must fail locally.",
                CommissionDetailState.Phase.ERROR,
                state.phase());
        assertEquals(
                "Invalid identities must use the public failure taxonomy.",
                Optional.of(CommissionFailureKind.INVALID_REQUEST),
                state.failure());
        assertEquals(
                "Invalid identities must never reach the repository.",
                0,
                repository.detailCalls);
    }

    @Test
    public void preservesTheExplicitScopeOnDetailLookup() {
        FakeRepository repository = new FakeRepository();
        CommissionDetailController controller = new CommissionDetailController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.load(701, CommissionScope.TEAM);

        assertEquals(
                "The selected list scope must be revalidated by the detail endpoint.",
                CommissionScope.TEAM,
                repository.scope);
        assertEquals(
                "A valid detail must transition to ready.",
                CommissionDetailState.Phase.READY,
                state(controller).phase());
    }

    private static CommissionDetailState state(
            CommissionDetailController controller) {
        CommissionDetailState[] value = new CommissionDetailState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static CommissionDetail detail() {
        CommissionSummary summary = new CommissionSummary(
                701,
                CommissionKind.SELLER,
                CommissionStatus.APPROVED,
                new CommissionSummary.Recipient(
                        OptionalLong.of(21),
                        CommissionRecipientRole.SALESPERSON,
                        Optional.of("Ana")),
                Optional.of(new CommissionSummary.Organization(9, "Loja Centro")),
                Optional.empty(),
                new CommissionSummary.Calculation(
                        "BRL",
                        new BigDecimal("100.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        "seller-v1"),
                2,
                false,
                Set.of(CommissionAction.PAY),
                Optional.of(Instant.parse("2026-07-25T13:00:00Z")),
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-07-25T12:00:00Z"),
                Instant.parse("2026-07-25T13:00:00Z"));
        return new CommissionDetail(
                summary,
                Optional.of("commission-701"),
                OptionalLong.empty(),
                Optional.of(new CommissionDetail.Actor(
                        1,
                        Optional.of("Gestor"))),
                Optional.empty(),
                Optional.empty());
    }

    private static final class FakeRepository implements CommissionRepository {
        private int detailCalls;
        private CommissionScope scope;

        @Override
        public CommissionPage page(
                CommissionQuery query,
                Optional<String> cursor) {
            return new CommissionPage(
                    List.of(),
                    new CommissionOverview(
                            new CommissionOverview.Totals(
                                    0,
                                    BigDecimal.ZERO),
                            new CommissionOverview.Totals(
                                    0,
                                    BigDecimal.ZERO),
                            new CommissionOverview.Totals(
                                    0,
                                    BigDecimal.ZERO),
                            new CommissionOverview.Totals(
                                    0,
                                    BigDecimal.ZERO)),
                    Optional.empty());
        }

        @Override
        public CommissionDetail detail(
                long commissionId,
                CommissionScope requestedScope) {
            detailCalls++;
            scope = requestedScope;
            return CommissionDetailControllerTest.detail();
        }

        @Override
        public CommissionMutationResult transition(
                CommissionMutationCommand command,
                String idempotencyKey) {
            throw new AssertionError("Not used.");
        }
    }
}
