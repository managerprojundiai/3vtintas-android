package br.com.tresvtintas.mobile.feature.agent;

import java.util.Optional;

@FunctionalInterface
public interface AgentRuntimeProvider {
    Optional<AgentFeatureRuntime> agentRuntime();
}
