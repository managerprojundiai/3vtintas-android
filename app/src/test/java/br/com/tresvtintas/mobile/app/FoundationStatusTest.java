package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class FoundationStatusTest {
    @Test
    public void environmentIsNormalizedForDisplay() {
        assertEquals(
                "Environment labels must be normalized.",
                "STAGING",
                FoundationStatus.formatEnvironment(" staging "));
    }

    @Test
    public void missingEnvironmentIsExplicit() {
        assertEquals(
                "Missing build configuration must be visible.",
                "DESCONHECIDO",
                FoundationStatus.formatEnvironment(null));
    }
}
