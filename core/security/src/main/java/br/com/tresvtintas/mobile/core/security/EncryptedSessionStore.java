package br.com.tresvtintas.mobile.core.security;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Atomic session vault whose plaintext is protected by an Android Keystore AES-GCM key.
 */
public final class EncryptedSessionStore {
    private static final String DEFAULT_NAMESPACE = "default";
    private static final int MAXIMUM_FILE_BYTES = 16_384;
    private final AtomicBinaryFile storage;
    private final AeadCipher cipher;
    private final byte[] associatedData;
    private final SessionCodec sessionCodec = new SessionCodec();
    private final EncryptedPayloadCodec payloadCodec = new EncryptedPayloadCodec();

    public EncryptedSessionStore(Context context) {
        this(context, DEFAULT_NAMESPACE);
    }

    public EncryptedSessionStore(Context context, String namespace) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required.");
        }
        if (namespace == null || !namespace.matches("[a-z0-9-]{3,40}")) {
            throw new IllegalArgumentException("Session namespace is invalid.");
        }
        Context applicationContext = context.getApplicationContext();
        File file = new File(
                applicationContext.getNoBackupFilesDir(),
                "mobile-session-v1-" + namespace + ".enc");
        this.storage = new AtomicBinaryFile(file, MAXIMUM_FILE_BYTES);
        this.cipher = new AndroidKeystoreAead("3v.mobile.session.aes.v1." + namespace);
        this.associatedData = (
                "3v:mobile:session:v1:"
                        + applicationContext.getPackageName()
                        + ":"
                        + namespace)
                .getBytes(StandardCharsets.UTF_8);
    }

    public synchronized void save(PersistedSession session) {
        byte[] plaintext = sessionCodec.encode(session);
        try {
            EncryptedPayload encrypted = cipher.encrypt(plaintext, associatedData);
            storage.write(payloadCodec.encode(encrypted));
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public synchronized Optional<PersistedSession> load() {
        Optional<byte[]> encoded = storage.read();
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        EncryptedPayload encrypted = payloadCodec.decode(encoded.get());
        byte[] plaintext = cipher.decrypt(encrypted, associatedData);
        try {
            return Optional.of(sessionCodec.decode(plaintext));
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public synchronized void clear() {
        storage.delete();
    }
}
