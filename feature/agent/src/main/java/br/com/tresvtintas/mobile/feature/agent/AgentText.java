package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class AgentText {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    private AgentText() {
        throw new AssertionError("No instances.");
    }

    static String activity(Instant value) {
        return DATE_TIME.format(value);
    }

    static int failure(AgentFailureKind kind) {
        return switch (kind) {
            case ACCESS_REVOKED -> R.string.agent_error_access;
            case AUTH_REJECTED -> R.string.agent_error_auth;
            case FORBIDDEN -> R.string.agent_error_forbidden;
            case NOT_FOUND -> R.string.agent_error_not_found;
            case INVALID_REQUEST -> R.string.agent_error_invalid;
            case CONFLICT -> R.string.agent_error_conflict;
            case IDEMPOTENCY_IN_PROGRESS, IDEMPOTENCY_KEY_REUSED ->
                    R.string.agent_error_idempotency;
            case RATE_LIMITED -> R.string.agent_error_rate_limit;
            case NETWORK -> R.string.agent_error_network;
            case SERVICE_UNAVAILABLE -> R.string.agent_error_service;
            case PROTOCOL -> R.string.agent_error_protocol;
            case UPDATE_REQUIRED -> R.string.agent_error_update;
        };
    }

    static boolean retryable(AgentFailureKind kind) {
        return kind == AgentFailureKind.NETWORK
                || kind == AgentFailureKind.RATE_LIMITED
                || kind == AgentFailureKind.SERVICE_UNAVAILABLE
                || kind == AgentFailureKind.IDEMPOTENCY_IN_PROGRESS;
    }
}
