package br.com.tresvtintas.mobile.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class EncryptedPayloadCodec {
    private static final int MAGIC = 0x33565345;
    private static final int VERSION = 1;
    private static final int INITIALIZATION_VECTOR_BYTES = 12;
    private static final int MINIMUM_CIPHERTEXT_BYTES = 16;
    private static final int MAXIMUM_CIPHERTEXT_BYTES = 12_000;

    byte[] encode(EncryptedPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Encrypted payload is required.");
        }
        byte[] initializationVector = payload.initializationVector();
        byte[] ciphertext = payload.ciphertext();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(initializationVector.length);
            output.write(initializationVector);
            output.writeInt(ciphertext.length);
            output.write(ciphertext);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new SessionStorageException("Could not encode encrypted state.", exception);
        }
    }

    EncryptedPayload decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw new SessionStorageException("Encrypted state is empty.");
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(encoded);
                DataInputStream input = new DataInputStream(bytes)) {
            requireHeader(input);
            byte[] initializationVector = readBytes(
                    input, input.readInt(), INITIALIZATION_VECTOR_BYTES, INITIALIZATION_VECTOR_BYTES);
            byte[] ciphertext = readBytes(
                    input, input.readInt(), MINIMUM_CIPHERTEXT_BYTES, MAXIMUM_CIPHERTEXT_BYTES);
            if (bytes.available() != 0) {
                throw new SessionStorageException("Encrypted state contains trailing data.");
            }
            return new EncryptedPayload(initializationVector, ciphertext);
        } catch (IOException | IllegalArgumentException exception) {
            throw new SessionStorageException("Encrypted state is invalid.", exception);
        }
    }

    private static void requireHeader(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC || input.readInt() != VERSION) {
            throw new SessionStorageException("Unsupported encrypted state format.");
        }
    }

    private static byte[] readBytes(
            DataInputStream input, int length, int minimum, int maximum) throws IOException {
        if (length < minimum || length > maximum) {
            throw new SessionStorageException("Encrypted state field has an invalid size.");
        }
        byte[] result = new byte[length];
        input.readFully(result);
        return result;
    }
}
