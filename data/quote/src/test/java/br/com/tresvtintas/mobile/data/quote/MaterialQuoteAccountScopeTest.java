package br.com.tresvtintas.mobile.data.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class MaterialQuoteAccountScopeTest {
    @Test
    public void bindsRepositoryLifetimeToAuthorizationRevision() {
        MaterialQuoteAccountScope scope = new MaterialQuoteAccountScope(
                41,
                "a".repeat(64));
        assertEquals(
                "Scope key must bind user and authorization revision.",
                "41:" + "a".repeat(64),
                scope.accountKey());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialQuoteAccountScope(41, "stale"));
    }
}
