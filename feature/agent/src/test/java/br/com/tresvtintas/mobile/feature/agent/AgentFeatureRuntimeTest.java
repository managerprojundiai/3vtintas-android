package br.com.tresvtintas.mobile.feature.agent;

import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.agent.AgentTurnCoordinator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;

public final class AgentFeatureRuntimeTest {
    @Test
    public void rejectsMissingRepository() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AgentTurnCoordinator coordinator =
                coordinator(scheduler);
        try {
            assertThrows(
                    "Agent runtime must require its repository.",
                    NullPointerException.class,
                    () -> new AgentFeatureRuntime(
                            null,
                            coordinator,
                            (context, document) -> {
                            },
                            (context, quoteId) -> {
                            },
                            Runnable::run,
                            (activity, serverClientId, nonce, callback) -> {
                            }));
        } finally {
            coordinator.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    public void rejectsMissingCoordinator() {
        assertThrows(
                "Agent runtime must require its coordinator.",
                NullPointerException.class,
                () -> new AgentFeatureRuntime(
                        new NoOpAgentRepository(),
                        null,
                        (context, document) -> {
                        },
                        (context, quoteId) -> {
                        },
                        Runnable::run,
                        (activity, serverClientId, nonce, callback) -> {
                        }));
    }

    @Test
    public void rejectsMissingDocumentNavigator() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AgentTurnCoordinator coordinator = coordinator(scheduler);
        try {
            assertThrows(
                    "Agent runtime must require document navigation.",
                    NullPointerException.class,
                    () -> new AgentFeatureRuntime(
                            new NoOpAgentRepository(),
                            coordinator,
                            null,
                            (context, quoteId) -> {
                            },
                            Runnable::run,
                            (activity, serverClientId, nonce, callback) -> {
                            }));
        } finally {
            coordinator.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    public void rejectsMissingQuoteNavigator() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AgentTurnCoordinator coordinator = coordinator(scheduler);
        try {
            assertThrows(
                    "Agent runtime must require quote navigation.",
                    NullPointerException.class,
                    () -> new AgentFeatureRuntime(
                            new NoOpAgentRepository(),
                            coordinator,
                            (context, document) -> {
                            },
                            null,
                            Runnable::run,
                            (activity, serverClientId, nonce, callback) -> {
                            }));
        } finally {
            coordinator.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    public void rejectsMissingGoogleCredentialRequester() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AgentTurnCoordinator coordinator = coordinator(scheduler);
        try {
            assertThrows(
                    "Agent runtime must require explicit Google confirmation.",
                    NullPointerException.class,
                    () -> new AgentFeatureRuntime(
                            new NoOpAgentRepository(),
                            coordinator,
                            (context, document) -> {
                            },
                            (context, quoteId) -> {
                            },
                            Runnable::run,
                            null));
        } finally {
            coordinator.close();
            scheduler.shutdownNow();
        }
    }

    private static AgentTurnCoordinator coordinator(
            ScheduledExecutorService scheduler) {
        return new AgentTurnCoordinator(
                new NoOpAgentRepository(),
                (turnId, cursor, listener) -> () -> {
                },
                Runnable::run,
                scheduler,
                Runnable::run);
    }
}
