package br.com.tresvtintas.mobile.feature.finance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import org.junit.Test;

public final class FinanceMutationAttemptTest {
    @Test
    public void reusesKeyOnlyForTheSameLogicalMutation() {
        FinanceMutationAttempt attempt = new FinanceMutationAttempt();

        String first = attempt.keyFor(
                FinanceAction.SETTLE,
                701,
                "PIX\ncomprovante");
        String retry = attempt.keyFor(
                FinanceAction.SETTLE,
                701,
                "PIX\ncomprovante");
        String changed = attempt.keyFor(
                FinanceAction.SETTLE,
                701,
                "PIX\noutro");

        assertEquals(
                "Retrying the same logical mutation must reuse its key.",
                first,
                retry);
        assertNotEquals(
                "Changing the payload must create another key.",
                first,
                changed);
    }

    @Test
    public void invalidRestoredKeyIsNeverReused() {
        FinanceMutationAttempt attempt =
                FinanceMutationAttempt.restored("invalid", "stale");

        assertNotEquals(
                "Invalid restored keys must never be reused.",
                "invalid",
                attempt.keyFor(FinanceAction.CANCEL, 701, ""));
    }
}
