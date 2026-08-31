package br.com.tresvtintas.mobile.feature.customer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import org.junit.Test;

public final class CustomerMutationAttemptTest {
    @Test
    public void reusesKeyOnlyForTheSameLogicalDraft() {
        CustomerMutationAttempt attempt = new CustomerMutationAttempt();
        CustomerDraft first = draft("Cliente", "11999999999");

        String firstKey = attempt.keyFor(first);
        String retryKey = attempt.keyFor(first);
        String changedKey = attempt.keyFor(
                draft("Cliente", "11988887777"));

        assertEquals(
                "Network retry of the same logical draft must reuse the key.",
                firstKey,
                retryKey);
        assertNotEquals(
                "Edited payload starts another logical mutation.",
                firstKey,
                changedKey);
    }

    @Test
    public void terminalFailureStartsANewLogicalAttempt() {
        CustomerMutationAttempt attempt = new CustomerMutationAttempt();
        CustomerDraft draft = draft("Cliente", "");
        String first = attempt.keyFor(draft);

        attempt.reset();

        assertNotEquals(
                "Terminal response cannot poison a deliberate retry.",
                first,
                attempt.keyFor(draft));
    }

    @Test
    public void retainsOnlyKeyAndDigestAcrossConfigurationChange() {
        CustomerMutationAttempt first = new CustomerMutationAttempt();
        CustomerDraft draft = draft("Cliente Sensível", "11999999999");
        String key = first.keyFor(draft);

        CustomerMutationAttempt restored = CustomerMutationAttempt.restored(
                first.key(),
                first.fingerprint());

        assertEquals(
                "In-memory configuration recreation preserves the retry key.",
                key,
                restored.keyFor(draft));
        assertEquals(
                "Fingerprint is a fixed SHA-256 digest.",
                64,
                restored.fingerprint().length());
    }

    private static CustomerDraft draft(String name, String phone) {
        return CustomerDraft.fromRaw(
                name,
                "",
                phone,
                "",
                "",
                "",
                "",
                "");
    }
}
