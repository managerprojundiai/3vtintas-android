package br.com.tresvtintas.mobile.core.security;

/**
 * Authenticated encryption boundary used by the session vault.
 */
public interface AeadCipher {
    EncryptedPayload encrypt(byte[] plaintext, byte[] associatedData);

    byte[] decrypt(EncryptedPayload payload, byte[] associatedData);
}
