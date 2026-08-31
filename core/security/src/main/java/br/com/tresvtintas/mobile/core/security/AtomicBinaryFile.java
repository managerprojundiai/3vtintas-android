package br.com.tresvtintas.mobile.core.security;

import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Small bounded atomic file primitive. Callers provide synchronization.
 */
final class AtomicBinaryFile {
    private static final int BUFFER_SIZE = 1024;
    private final AtomicFile atomicFile;
    private final int maximumBytes;

    AtomicBinaryFile(File file, int maximumBytes) {
        if (file == null || maximumBytes < 1) {
            throw new IllegalArgumentException("Valid file and size limit are required.");
        }
        this.atomicFile = new AtomicFile(file);
        this.maximumBytes = maximumBytes;
    }

    Optional<byte[]> read() {
        try (InputStream input = atomicFile.openRead();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int total = 0;
            int read = input.read(buffer);
            while (read != -1) {
                total += read;
                if (total > maximumBytes) {
                    throw new SessionStorageException("Protected state exceeds its size limit.");
                }
                output.write(buffer, 0, read);
                read = input.read(buffer);
            }
            return Optional.of(output.toByteArray());
        } catch (FileNotFoundException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new SessionStorageException("Could not read protected local state.", exception);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    void write(byte[] content) {
        if (content == null || content.length == 0 || content.length > maximumBytes) {
            throw new IllegalArgumentException("Protected state has an invalid size.");
        }
        FileOutputStream output = null;
        try {
            output = atomicFile.startWrite();
            output.write(content);
            output.flush();
            atomicFile.finishWrite(output);
        } catch (IOException exception) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw new SessionStorageException("Could not write protected local state.", exception);
        }
    }

    void delete() {
        atomicFile.delete();
    }
}
