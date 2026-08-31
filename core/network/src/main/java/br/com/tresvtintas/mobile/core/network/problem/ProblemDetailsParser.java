package br.com.tresvtintas.mobile.core.network.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Bounded parser for non-successful mobile API responses.
 */
public final class ProblemDetailsParser {
    private static final int MAXIMUM_BODY_BYTES = 16_384;
    private static final int BUFFER_BYTES = 1024;
    private final ObjectMapper objectMapper;

    public ProblemDetailsParser(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("Object mapper is required.");
        }
        this.objectMapper = objectMapper.copy();
    }

    public Optional<ProblemDetails> parse(Response<?> response) {
        if (response == null || response.isSuccessful()) {
            return Optional.empty();
        }
        if (response.errorBody() == null) {
            return Optional.empty();
        }
        try (ResponseBody body = response.errorBody()) {
            if (!isProblemJson(body.contentType())
                    || body.contentLength() > MAXIMUM_BODY_BYTES) {
                return Optional.empty();
            }
            byte[] bytes = readBounded(body.byteStream());
            return Optional.of(objectMapper.readValue(bytes, ProblemDetails.class));
        } catch (IOException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean isProblemJson(MediaType mediaType) {
        return mediaType != null
                && "application".equalsIgnoreCase(mediaType.type())
                && "problem+json".equalsIgnoreCase(mediaType.subtype());
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_BYTES];
            int total = 0;
            int read = input.read(buffer);
            while (read != -1) {
                total += read;
                if (total > MAXIMUM_BODY_BYTES) {
                    throw new IOException("Problem response exceeds its size limit.");
                }
                output.write(buffer, 0, read);
                read = input.read(buffer);
            }
            return output.toByteArray();
        }
    }
}
