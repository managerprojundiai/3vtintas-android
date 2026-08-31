package br.com.tresvtintas.mobile.core.security;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the stable, non-backed-up UUID v4 for this app installation.
 */
public final class InstallationIdentityStore {
    private static final String FILE_NAME = "installation-identity-v1";
    private static final int MAXIMUM_BYTES = 64;
    private final AtomicBinaryFile storage;

    public InstallationIdentityStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required.");
        }
        Context applicationContext = context.getApplicationContext();
        File file = new File(applicationContext.getNoBackupFilesDir(), FILE_NAME);
        this.storage = new AtomicBinaryFile(file, MAXIMUM_BYTES);
    }

    public synchronized String getOrCreate() {
        Optional<byte[]> existing = storage.read();
        if (existing.isPresent()) {
            String value = new String(existing.get(), StandardCharsets.US_ASCII);
            try {
                return UuidV4.require(value, "Installation ID");
            } catch (IllegalArgumentException exception) {
                throw new SessionStorageException(
                        "Installation identity is corrupt; explicit recovery is required.",
                        exception);
            }
        }
        String generated = UUID.randomUUID().toString();
        storage.write(generated.getBytes(StandardCharsets.US_ASCII));
        return generated;
    }

    public synchronized void reset() {
        storage.delete();
    }
}
