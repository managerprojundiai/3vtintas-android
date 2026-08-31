package br.com.tresvtintas.mobile.core.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class PreconditionsTest {
    @Test
    public void requireNonBlankReturnsValidText() {
        assertEquals(
                "Valid text must pass through unchanged.",
                "3V",
                Preconditions.requireNonBlank("3V", "brand"));
    }

    @Test
    public void requireNonBlankRejectsWhitespace() {
        assertThrows(
                "Whitespace-only values must be rejected.",
                IllegalArgumentException.class,
                () -> Preconditions.requireNonBlank("  ", "brand"));
    }
}
