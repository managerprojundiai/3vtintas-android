package br.com.tresvtintas.mobile.feature.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import java.time.Instant;
import org.junit.Test;

public final class AgentTextTest {
    @Test
    public void formatsActivityInCanonicalBusinessTimezone() {
        assertEquals(
                "Agent activity must use the São Paulo business time.",
                "27/07/2026 às 09:00",
                AgentText.activity(
                        Instant.parse("2026-07-27T12:00:00Z")));
    }

    @Test
    public void exposesRetryOnlyForTransientFailures() {
        assertTrue(
                "Network failures must allow retry.",
                AgentText.retryable(AgentFailureKind.NETWORK));
        assertTrue(
                "Service failures must allow retry.",
                AgentText.retryable(
                        AgentFailureKind.SERVICE_UNAVAILABLE));
        assertTrue(
                "In-progress idempotency must allow retry.",
                AgentText.retryable(
                        AgentFailureKind.IDEMPOTENCY_IN_PROGRESS));
        assertFalse(
                "Authorization failures must not loop locally.",
                AgentText.retryable(AgentFailureKind.FORBIDDEN));
        assertFalse(
                "Protocol failures require a client correction.",
                AgentText.retryable(AgentFailureKind.PROTOCOL));
    }

    @Test
    public void mapsAccessRevocationToDedicatedMessage() {
        assertEquals(
                "Access revocation must use the explicit safe message.",
                R.string.agent_error_access,
                AgentText.failure(
                        AgentFailureKind.ACCESS_REVOKED));
    }
}
