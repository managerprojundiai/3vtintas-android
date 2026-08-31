package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentAction;

@FunctionalInterface
interface AgentActionReviewListener {
    void review(AgentAction action);
}
