package br.com.tresvtintas.mobile.feature.commission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.commission.CommissionMutationCommand;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import org.junit.Test;

public final class CommissionActionAttemptTest {
    @Test
    public void reusesTheKeyOnlyForTheSameLogicalPayload() {
        CommissionActionAttempt attempt = new CommissionActionAttempt();
        CommissionMutationCommand first = CommissionMutationCommand.pay(
                701,
                2,
                CommissionPaymentMethod.PIX,
                "PIX-701");

        String original = attempt.keyFor(first);
        String retry = attempt.keyFor(first);
        String changed = attempt.keyFor(CommissionMutationCommand.pay(
                701,
                2,
                CommissionPaymentMethod.TRANSFER,
                "TED-701"));

        assertEquals(
                "A logical retry must reuse the exact idempotency key.",
                original,
                retry);
        assertNotEquals(
                "A changed payload must receive a new idempotency key.",
                original,
                changed);
    }

    @Test
    public void restoresOnlyValidatedHashedState() {
        CommissionMutationCommand command =
                CommissionMutationCommand.cancel(
                        701,
                        1,
                        "Pedido estornado pela loja");
        CommissionActionAttempt source = new CommissionActionAttempt();
        String key = source.keyFor(command);

        CommissionActionAttempt restored =
                CommissionActionAttempt.restored(
                        key,
                        source.fingerprint());
        CommissionActionAttempt rejected =
                CommissionActionAttempt.restored(
                        key,
                        "Pedido estornado pela loja");

        assertEquals(
                "A valid hashed fingerprint must survive recreation.",
                key,
                restored.keyFor(command));
        assertNotEquals(
                "Raw form data must never be accepted as saved state.",
                key,
                rejected.keyFor(command));
    }
}
