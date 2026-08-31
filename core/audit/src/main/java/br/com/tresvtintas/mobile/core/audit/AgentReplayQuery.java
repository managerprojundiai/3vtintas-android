package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.ChannelFilter;
import java.util.Objects;

public record AgentReplayQuery(ChannelFilter channel, int pageSize) {
    public AgentReplayQuery {
        Objects.requireNonNull(channel, "Agent replay channel filter is required.");
        if (pageSize < 1 || pageSize > 30) {
            throw new IllegalArgumentException("Agent replay page size is invalid.");
        }
    }

    public static AgentReplayQuery initial() {
        return new AgentReplayQuery(ChannelFilter.ALL, 15);
    }

    public AgentReplayQuery withChannel(ChannelFilter value) {
        return new AgentReplayQuery(value, pageSize);
    }
}
