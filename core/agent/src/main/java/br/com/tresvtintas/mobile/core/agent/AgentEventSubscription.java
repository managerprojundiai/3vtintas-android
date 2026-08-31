package br.com.tresvtintas.mobile.core.agent;

@FunctionalInterface
public interface AgentEventSubscription extends AutoCloseable {
    @Override
    void close();
}
