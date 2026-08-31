package br.com.tresvtintas.mobile.core.security;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import org.junit.Test;

public final class EncryptedPayloadCodecTest {
    @Test
    public void roundTripsBoundedEncryptedEnvelope() {
        byte[] initializationVector = new byte[12];
        byte[] ciphertext = new byte[48];
        Arrays.fill(initializationVector, (byte) 3);
        Arrays.fill(ciphertext, (byte) 7);
        EncryptedPayloadCodec codec = new EncryptedPayloadCodec();

        EncryptedPayload decoded = codec.decode(
                codec.encode(new EncryptedPayload(initializationVector, ciphertext)));

        assertArrayEquals(
                "Initialization vector must survive envelope encoding.",
                initializationVector,
                decoded.initializationVector());
        assertCiphertextEquals(ciphertext, decoded);
    }

    @Test(expected = SessionStorageException.class)
    public void rejectsTrailingData() {
        EncryptedPayloadCodec codec = new EncryptedPayloadCodec();
        byte[] encoded = codec.encode(new EncryptedPayload(new byte[12], new byte[16]));
        codec.decode(Arrays.copyOf(encoded, encoded.length + 1));
    }

    private static void assertCiphertextEquals(
            byte[] expected, EncryptedPayload decoded) {
        assertArrayEquals(
                "Ciphertext must survive envelope encoding.",
                expected,
                decoded.ciphertext());
    }
}
