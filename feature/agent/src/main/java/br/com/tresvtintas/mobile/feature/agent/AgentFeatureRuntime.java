package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentRepository;
import br.com.tresvtintas.mobile.core.agent.AgentTurnCoordinator;
import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import java.util.Objects;
import java.util.concurrent.Executor;

public record AgentFeatureRuntime(
        AgentRepository repository,
        AgentTurnCoordinator turnCoordinator,
        AgentDocumentNavigator documentNavigator,
        AgentQuoteNavigator quoteNavigator,
        Executor workerExecutor,
        GoogleIdTokenRequester googleIdTokenRequester) {
    public AgentFeatureRuntime {
        repository = Objects.requireNonNull(
                repository,
                "Agent repository is required.");
        turnCoordinator = Objects.requireNonNull(
                turnCoordinator,
                "Agent turn coordinator is required.");
        documentNavigator = Objects.requireNonNull(
                documentNavigator,
                "Agent document navigator is required.");
        quoteNavigator = Objects.requireNonNull(
                quoteNavigator,
                "Agent quote navigator is required.");
        workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "Agent worker is required.");
        googleIdTokenRequester = Objects.requireNonNull(
                googleIdTokenRequester,
                "Agent Google credential requester is required.");
    }
}
