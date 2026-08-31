package br.com.tresvtintas.mobile.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;

final class SessionCodec {
    private static final int MAGIC = 0x33565350;
    private static final int VERSION = 1;

    byte[] encode(PersistedSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required.");
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeUTF(session.refreshToken());
            writeInstant(output, session.refreshTokenExpiresAt());
            writeInstant(output, session.sessionAbsoluteExpiresAt());
            output.writeUTF(session.sessionId());
            output.writeUTF(session.deviceId());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new SessionStorageException("Could not encode protected session state.", exception);
        }
    }

    PersistedSession decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw new SessionStorageException("Protected session payload is empty.");
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(encoded);
                DataInputStream input = new DataInputStream(bytes)) {
            requireHeader(input);
            PersistedSession session = new PersistedSession(
                    input.readUTF(),
                    readInstant(input),
                    readInstant(input),
                    input.readUTF(),
                    input.readUTF());
            if (bytes.available() != 0) {
                throw new SessionStorageException(
                        "Protected session payload contains trailing data.");
            }
            return session;
        } catch (IOException | IllegalArgumentException exception) {
            throw new SessionStorageException("Protected session payload is invalid.", exception);
        }
    }

    private static void requireHeader(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC || input.readInt() != VERSION) {
            throw new SessionStorageException("Unsupported protected session format.");
        }
    }

    private static void writeInstant(DataOutputStream output, Instant instant)
            throws IOException {
        output.writeLong(instant.getEpochSecond());
        output.writeInt(instant.getNano());
    }

    private static Instant readInstant(DataInputStream input) throws IOException {
        return Instant.ofEpochSecond(input.readLong(), input.readInt());
    }
}
