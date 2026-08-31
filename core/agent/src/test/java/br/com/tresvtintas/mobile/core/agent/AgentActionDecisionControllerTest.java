package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import org.junit.Test;

public final class AgentActionDecisionControllerTest {
    private static final Instant NOW =
            Instant.parse("2026-07-27T12:00:00Z");
    private static final String KEY =
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";

    @Test
    public void forwardsOneExplicitDecisionAndPublishesSuccess() {
        StubRepository repository = new StubRepository();
        List<AgentActionDecisionState> states = new ArrayList<>();
        AgentActionDecisionController controller = controller(repository);
        controller.subscribe(states::add);
        AgentAction action = AgentActionTest.action(
                AgentActionStatus.PENDING,
                NOW.plusSeconds(60));

        controller.decide(
                action,
                AgentActionDecision.CONFIRM,
                KEY);

        assertEquals(
                "The opaque action ID must be forwarded unchanged.",
                action.id(),
                repository.actionId);
        assertEquals(
                "The explicit decision must be forwarded unchanged.",
                AgentActionDecision.CONFIRM,
                repository.decision);
        assertEquals(
                "The logical attempt must keep its caller key.",
                KEY,
                repository.idempotencyKey);
        assertEquals(
                "A completed mutation must publish terminal success.",
                AgentActionDecisionState.Phase.SUCCESS,
                states.get(states.size() - 1).phase());
        assertTrue(
                "The result must expose the executed state.",
                states.get(states.size() - 1)
                        .result()
                        .orElseThrow()
                        .action()
                        .status()
                        .terminal());
    }

    @Test
    public void expiresLocallyWithoutCallingTheRepository() {
        StubRepository repository = new StubRepository();
        List<AgentActionDecisionState> states = new ArrayList<>();
        AgentActionDecisionController controller = controller(repository);
        controller.subscribe(states::add);
        AgentAction expired = AgentActionTest.action(
                AgentActionStatus.PENDING,
                NOW);

        controller.decide(
                expired,
                AgentActionDecision.REJECT,
                KEY);

        assertFalse(
                "An expired action must not reach the mutation endpoint.",
                repository.called);
        AgentActionDecisionState result =
                states.get(states.size() - 1);
        assertEquals(
                "Local expiry must trigger a server refresh path.",
                AgentActionDecisionState.Phase.ERROR,
                result.phase());
        assertEquals(
                "Local expiry is a stale-resource conflict.",
                Optional.of(AgentFailureKind.CONFLICT),
                result.failure());
    }

    @Test
    public void refusesProtectedConfirmationWithoutStepUpToken() {
        StubRepository repository = new StubRepository();
        List<AgentActionDecisionState> states = new ArrayList<>();
        AgentActionDecisionController controller = controller(repository);
        controller.subscribe(states::add);

        controller.decide(
                protectedAction(),
                AgentActionDecision.CONFIRM,
                KEY);

        assertFalse(
                "A protected mutation must never reach transport without step-up.",
                repository.called);
        assertEquals(
                "Missing step-up is an invalid local request.",
                Optional.of(AgentFailureKind.INVALID_REQUEST),
                states.get(states.size() - 1).failure());
    }

    @Test
    public void forwardsOneEphemeralStepUpTokenForProtectedConfirmation() {
        StubRepository repository = new StubRepository();
        AgentActionDecisionController controller = controller(repository);
        String token = "3vsu1_" + "a".repeat(43);

        controller.decide(
                protectedAction(),
                AgentActionDecision.CONFIRM,
                KEY,
                Optional.of(token));

        assertTrue(
                "A valid protected confirmation must reach transport.",
                repository.called);
        assertEquals(
                "The ephemeral token must be forwarded unchanged.",
                Optional.of(token),
                repository.stepUpToken);
    }

    private static AgentActionDecisionController controller(
            StubRepository repository) {
        return new AgentActionDecisionController(
                repository,
                Runnable::run,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AgentAction protectedAction() {
        return new AgentAction(
                "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                AgentActionKind.PERSONAL_FINANCE_CREATE,
                AgentActionStatus.PENDING,
                AgentAction.PERSONAL_FINANCE_CREATE_TITLE,
                new AgentFinanceSummary(
                        AgentFinanceScope.PERSONAL,
                        AgentFinanceOperation.CREATE,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        FinanceEntryType.EXPENSE,
                        "Combustível",
                        new BigDecimal("80.00"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        FinanceEntryStatus.PENDING,
                        Optional.empty(),
                        Optional.empty(),
                        true),
                Optional.empty(),
                true,
                NOW.plusSeconds(60));
    }

    private static final class StubRepository
            implements AgentRepository {
        private boolean called;
        private String actionId;
        private AgentActionDecision decision;
        private String idempotencyKey;
        private Optional<String> stepUpToken = Optional.empty();

        @Override
        public AgentActionDecisionResult decideAction(
                String requestedActionId,
                AgentActionDecision requestedDecision,
                String requestedKey) {
            return decideAction(
                    requestedActionId,
                    requestedDecision,
                    requestedKey,
                    Optional.empty());
        }

        @Override
        public AgentActionDecisionResult decideAction(
                String requestedActionId,
                AgentActionDecision requestedDecision,
                String requestedKey,
                Optional<String> requestedStepUpToken) {
            called = true;
            actionId = requestedActionId;
            decision = requestedDecision;
            idempotencyKey = requestedKey;
            stepUpToken = requestedStepUpToken;
            return new AgentActionDecisionResult(
                    requestedStepUpToken.isPresent()
                            ? new AgentAction(
                                    protectedAction().id(),
                                    protectedAction().kind(),
                                    AgentActionStatus.EXECUTED,
                                    protectedAction().title(),
                                    protectedAction().summary(),
                                    Optional.of(new AgentFinanceResult(
                                            AgentFinanceScope.PERSONAL,
                                            31,
                                            FinanceEntryStatus.PENDING,
                                            true)),
                                    true,
                                    NOW.plusSeconds(60))
                            : AgentActionTest.action(
                                    requestedDecision
                                                    == AgentActionDecision.CONFIRM
                                            ? AgentActionStatus.EXECUTED
                                            : AgentActionStatus.REJECTED,
                                    NOW.plusSeconds(60)),
                    false);
        }

        @Override
        public AgentConversationPage conversations(
                Optional<String> cursor,
                int limit) {
            throw unused();
        }

        @Override
        public AgentConversation createConversation(
                String title,
                String key) {
            throw unused();
        }

        @Override
        public AgentConversation conversation(String conversationId) {
            throw unused();
        }

        @Override
        public AgentMessagePage messages(
                String conversationId,
                Optional<String> cursor,
                int limit) {
            throw unused();
        }

        @Override
        public AgentTurn enqueue(
                String conversationId,
                String message,
                String key) {
            throw unused();
        }

        @Override
        public AgentTurn turn(String turnId) {
            throw unused();
        }

        @Override
        public AgentTurn cancel(
                String turnId,
                String key) {
            throw unused();
        }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException(
                    "Not used by this test.");
        }
    }
}
