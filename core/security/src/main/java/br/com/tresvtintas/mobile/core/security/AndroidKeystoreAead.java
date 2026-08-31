package br.com.tresvtintas.mobile.core.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM implementation backed by a non-exportable Android Keystore key.
 */
public final class AndroidKeystoreAead implements AeadCipher {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AUTHENTICATION_TAG_BITS = 128;
    private final String keyAlias;

    public AndroidKeystoreAead(String keyAlias) {
        if (keyAlias == null || !keyAlias.matches("[A-Za-z0-9._-]{3,80}")) {
            throw new IllegalArgumentException("Invalid Android Keystore key alias.");
        }
        this.keyAlias = keyAlias;
    }

    @Override
    public EncryptedPayload encrypt(byte[] plaintext, byte[] associatedData) {
        requireInput(plaintext, associatedData);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey());
            cipher.updateAAD(associatedData);
            return new EncryptedPayload(cipher.getIV(), cipher.doFinal(plaintext));
        } catch (GeneralSecurityException | IOException exception) {
            throw new SessionStorageException("Could not encrypt protected session state.", exception);
        }
    }

    @Override
    public byte[] decrypt(EncryptedPayload payload, byte[] associatedData) {
        if (payload == null) {
            throw new IllegalArgumentException("Encrypted payload is required.");
        }
        requireAssociatedData(associatedData);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameters = new GCMParameterSpec(
                    AUTHENTICATION_TAG_BITS, payload.initializationVector());
            cipher.init(Cipher.DECRYPT_MODE, loadExistingKey(), parameters);
            cipher.updateAAD(associatedData);
            return cipher.doFinal(payload.ciphertext());
        } catch (GeneralSecurityException | IOException exception) {
            throw new SessionStorageException("Protected session state is not authentic.", exception);
        }
    }

    private SecretKey loadOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore();
        if (keyStore.containsAlias(keyAlias)) {
            return requireSecretKey(keyStore);
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return keyGenerator.generateKey();
    }

    private SecretKey loadExistingKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            throw new GeneralSecurityException("Android Keystore key is missing.");
        }
        return requireSecretKey(keyStore);
    }

    private SecretKey requireSecretKey(KeyStore keyStore)
            throws GeneralSecurityException {
        java.security.Key key = keyStore.getKey(keyAlias, null);
        if (!(key instanceof SecretKey)) {
            throw new GeneralSecurityException("Android Keystore entry is not a secret key.");
        }
        return (SecretKey) key;
    }

    private static KeyStore loadKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }

    private static void requireInput(byte[] plaintext, byte[] associatedData) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("Plaintext is required.");
        }
        requireAssociatedData(associatedData);
    }

    private static void requireAssociatedData(byte[] associatedData) {
        if (associatedData == null || associatedData.length == 0) {
            throw new IllegalArgumentException("Associated data is required.");
        }
    }
}
