package br.com.tresvtintas.mobile.data.commission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class CommissionAccountScopeTest {
    @Test
    public void bindsRepositoryLifetimeToUserAndAuthorizationRevision() {
        CommissionAccountScope scope = new CommissionAccountScope(
                21,
                "a".repeat(64));

        assertEquals(
                "The user identity must be retained for scope comparison.",
                21,
                scope.userId());
        assertThrows(
                "An invalid authorization revision must fail closed.",
                IllegalArgumentException.class,
                () -> new CommissionAccountScope(21, "stale"));
        assertThrows(
                "An invalid user identity must fail closed.",
                IllegalArgumentException.class,
                () -> new CommissionAccountScope(0, "a".repeat(64)));
    }
}
