package br.com.tresvtintas.mobile.core.agent;

import java.util.Optional;

@FunctionalInterface
public interface AgentEventSource {
    interface Listener {
        void onEvent(AgentEvent event);

        void onFailure(AgentException failure);

        void onClosed();
    }

    AgentEventSubscription subscribe(
            String turnId,
            Optional<String> lastEventId,
            Listener listener) throws AgentException;
}
