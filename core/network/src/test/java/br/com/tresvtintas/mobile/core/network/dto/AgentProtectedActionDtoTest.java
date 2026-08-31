package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AgentProtectedActionDtoTest {
    private static final String ACTION_ID =
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String EXPIRY =
            "2026-07-29T13:00:00Z";
    private static final String STEP_UP_TOKEN =
            "3vsu1_" + "a".repeat(43);

    @Test
    public void acceptsProtectedFinanceActionOnlyWithServerPolicy() {
        AgentActionSummaryDto summary = financeSummary();
        AgentActionDto action = new AgentActionDto(
                ACTION_ID,
                "personal_finance_create",
                "pending",
                "Criar lançamento financeiro pessoal",
                summary,
                null,
                true,
                EXPIRY);

        assertTrue(
                "Protected finance policy must remain explicit.",
                action.requiresStepUp());
        assertEquals(
                "The nested finance summary must remain available.",
                "80.00",
                action.summary().finance().amount());
        assertThrows(
                "The wire response cannot downgrade protected finance.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        ACTION_ID,
                        "personal_finance_create",
                        "pending",
                        "Criar lançamento financeiro pessoal",
                        summary,
                        null,
                        false,
                        EXPIRY));
    }

    @Test
    public void validatesEphemeralStepUpEnvelope() {
        AgentStepUpChallengeResponse challenge =
                new AgentStepUpChallengeResponse(
                        "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                        "3vn1_" + "n".repeat(43),
                        "473842962788-example.apps.googleusercontent.com",
                        EXPIRY);
        AgentStepUpGrantResponse grant =
                new AgentStepUpGrantResponse(
                        STEP_UP_TOKEN,
                        EXPIRY);

        assertTrue(
                "The server nonce must remain bound to the challenge.",
                challenge.nonce().startsWith("3vn1_"));
        assertEquals(
                "The ephemeral grant must remain unchanged in memory.",
                STEP_UP_TOKEN,
                grant.stepUpToken());
        assertThrows(
                "A malformed step-up token must fail closed.",
                IllegalArgumentException.class,
                () -> new AgentStepUpGrantResponse(
                        "invalid",
                        EXPIRY));
    }

    @Test
    public void allowsStepUpTokenOnlyOnConfirmation() {
        AgentActionDecisionRequest request =
                new AgentActionDecisionRequest(
                        "confirm",
                        "CONFIRM_AGENT_ACTION",
                        STEP_UP_TOKEN);

        assertEquals(
                "Confirmation must retain the exact ephemeral grant.",
                STEP_UP_TOKEN,
                request.stepUpToken());
        assertThrows(
                "Rejecting an action must not transmit a step-up token.",
                IllegalArgumentException.class,
                () -> new AgentActionDecisionRequest(
                        "reject",
                        "REJECT_AGENT_ACTION",
                        STEP_UP_TOKEN));
    }

    private static AgentActionSummaryDto financeSummary() {
        AgentFinanceSummaryDto finance = new AgentFinanceSummaryDto(
                "personal",
                "create",
                null,
                null,
                null,
                18L,
                "Pintor de teste",
                "expense",
                "Combustível",
                "80.00",
                "BRL",
                null,
                "Visita técnica",
                null,
                "pending",
                null,
                null,
                true);
        return new AgentActionSummaryDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                finance,
                null);
    }
}
