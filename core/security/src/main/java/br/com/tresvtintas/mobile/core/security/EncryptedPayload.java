package br.com.tresvtintas.mobile.core.security;

import java.util.Arrays;

/**
 * Immutable AES-GCM output containing the public initialization vector and ciphertext.
 */
public final class EncryptedPayload {
    private final byte[] initializationVector;
    private final byte[] ciphertext;

    public EncryptedPayload(byte[] initializationVector, byte[] ciphertext) {
        if (initializationVector == null || initializationVector.length != 12) {
            throw new IllegalArgumentException("AES-GCM initialization vector must have 12 bytes.");
        }
        if (ciphertext == null || ciphertext.length < 16) {
            throw new IllegalArgumentException("AES-GCM ciphertext is invalid.");
        }
        this.initializationVector = Arrays.copyOf(
                initializationVector, initializationVector.length);
        this.ciphertext = Arrays.copyOf(ciphertext, ciphertext.length);
    }

    public byte[] initializationVector() {
        return Arrays.copyOf(initializationVector, initializationVector.length);
    }

    public byte[] ciphertext() {
        return Arrays.copyOf(ciphertext, ciphertext.length);
    }
}
