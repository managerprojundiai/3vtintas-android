package br.com.tresvtintas.mobile.app;

import android.content.Context;
import br.com.tresvtintas.mobile.core.agent.AgentDocument;
import br.com.tresvtintas.mobile.core.agent.AgentDocumentType;
import br.com.tresvtintas.mobile.core.agent.AgentTurnCoordinator;
import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.data.agent.AgentAccountScope;
import br.com.tresvtintas.mobile.data.agent.RemoteAgentEventSource;
import br.com.tresvtintas.mobile.data.agent.RemoteAgentRepository;
import br.com.tresvtintas.mobile.feature.agent.AgentFeatureRuntime;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteDetailActivity;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuoteDetailActivity;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Owns the personal-agent session and every transient conversation buffer. No prompt, streamed
 * response, event cursor, or idempotency key survives logout, authorization changes, or process
 * termination.
 */
final class AgentApplicationComponent implements AutoCloseable {
    private final MobileApi protectedApi;
    private final MobileNetworkClient protectedClient;
    private final ExecutorService worker;
    private final ScheduledExecutorService streamWorker;
    private final Executor callbackExecutor;
    private final GoogleIdTokenRequester googleIdTokenRequester;
    private Optional<Session> session = Optional.empty();

    AgentApplicationComponent(
            MobileNetworkClient protectedClient,
            Executor callbackExecutor,
            GoogleIdTokenRequester googleIdTokenRequester) {
        if (protectedClient == null) {
            throw new IllegalArgumentException(
                    "Protected agent client is required.");
        }
        this.protectedClient = protectedClient;
        protectedApi = protectedClient.api();
        this.callbackExecutor = java.util.Objects.requireNonNull(
                callbackExecutor,
                "Agent callback executor is required.");
        this.googleIdTokenRequester = java.util.Objects.requireNonNull(
                googleIdTokenRequester,
                "Agent Google credential requester is required.");
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-agent-worker");
            thread.setDaemon(false);
            return thread;
        });
        streamWorker = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-agent-stream");
                    thread.setDaemon(false);
                    return thread;
                });
    }

    synchronized void activate(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !access.bootstrap().authorization().has(
                        Capability.AGENT_USE)
                || worker.isShutdown()) {
            deactivate();
            return;
        }
        AgentAccountScope scope = new AgentAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (session.map(Session::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivate();
        RemoteAgentRepository repository =
                new RemoteAgentRepository(scope, protectedApi);
        RemoteAgentEventSource eventSource =
                new RemoteAgentEventSource(scope, protectedClient);
        AgentTurnCoordinator coordinator =
                new AgentTurnCoordinator(
                        repository,
                        eventSource,
                        worker,
                        streamWorker,
                        callbackExecutor);
        session = Optional.of(new Session(
                scope,
                repository,
                eventSource,
                coordinator,
                new AgentFeatureRuntime(
                        repository,
                        coordinator,
                        AgentApplicationComponent::openDocument,
                        AgentApplicationComponent::openMaterialQuote,
                        worker,
                        googleIdTokenRequester)));
    }

    private static void openDocument(
            Context context,
            AgentDocument document) {
        if (document.type() == AgentDocumentType.MATERIAL_QUOTE_PDF) {
            context.startActivity(MaterialQuoteDetailActivity.pdfIntent(
                    context,
                    document.quoteId()));
            return;
        }
        context.startActivity(LaborQuoteDetailActivity.pdfIntent(
                context,
                document.quoteId()));
    }

    private static void openMaterialQuote(
            Context context,
            long quoteId) {
        context.startActivity(MaterialQuoteDetailActivity.intent(
                context,
                quoteId));
    }

    synchronized void deactivate() {
        Optional<Session> previous = session;
        session = Optional.empty();
        previous.ifPresent(value -> {
            value.coordinator().close();
            value.eventSource().close();
            value.repository().close();
        });
    }

    synchronized Optional<AgentFeatureRuntime> runtime() {
        return session.map(Session::runtime);
    }

    @Override
    public synchronized void close() {
        deactivate();
        worker.shutdownNow();
        streamWorker.shutdownNow();
    }

    private record Session(
            AgentAccountScope scope,
            RemoteAgentRepository repository,
            RemoteAgentEventSource eventSource,
            AgentTurnCoordinator coordinator,
            AgentFeatureRuntime runtime) {
    }
}
