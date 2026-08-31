package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class AndroidDeviceInfoFactoryTest {
    @Test
    public void removesControlCharactersAndBoundsMetadata() {
        String normalized = AndroidDeviceInfoFactory.normalized(
                " 3V\tReference\u0000Device " + "x".repeat(200),
                32);

        assertEquals(32, normalized.length());
        assertEquals("3V Reference Device " + "x".repeat(12), normalized);
    }

    @Test
    public void mapsBlankMetadataToAbsentValue() {
        assertNull(AndroidDeviceInfoFactory.normalized(" \t\r\n", 40));
    }
}
