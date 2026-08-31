package br.com.tresvtintas.mobile.data.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteFailureKind;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePdfDownload;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

final class LaborQuotePdfTransport {
    private static final int BUFFER_BYTES = 16 * 1024;
    private static final long MAXIMUM_BYTES = 10L * 1024L * 1024L;
    private static final byte[] SIGNATURE = {37, 80, 68, 70, 45};
    private static final Pattern FILENAME = Pattern.compile(
            "(?i)(?:^|;)\\s*filename=\""
                    + "(orcamento-[0-9]{6,15}-labor\\.pdf)\"(?:;|$)");

    private LaborQuotePdfTransport() {
    }

    static LaborQuotePdfDownload download(
            Response<ResponseBody> response,
            OutputStream destination,
            ActiveScope scope) throws LaborQuoteException {
        try (ResponseBody body = requireBody(response);
             InputStream source = body.byteStream()) {
            validateHeaders(response, body);
            String filename = filename(response);
            return new LaborQuotePdfDownload(
                    filename,
                    stream(source, destination, scope));
        } catch (IOException exception) {
            throw network(exception);
        }
    }

    private static ResponseBody requireBody(Response<ResponseBody> response)
            throws LaborQuoteException {
        ResponseBody body = response.body();
        if (body == null) {
            throw protocol("Labor quote PDF body is absent.");
        }
        return body;
    }

    private static void validateHeaders(Response<?> response, ResponseBody body)
            throws LaborQuoteException {
        MediaType type = body.contentType();
        long length = body.contentLength();
        String cache = response.headers().get("Cache-Control");
        if (type == null
                || !"application".equalsIgnoreCase(type.type())
                || !"pdf".equalsIgnoreCase(type.subtype())
                || (length != -1 && (length < SIGNATURE.length || length > MAXIMUM_BYTES))
                || cache == null
                || !cache.toLowerCase(Locale.ROOT).contains("no-store")
                || !"nosniff".equalsIgnoreCase(
                        response.headers().get("X-Content-Type-Options"))) {
            throw protocol("Labor quote PDF headers are unsafe.");
        }
    }

    private static String filename(Response<?> response) throws LaborQuoteException {
        Matcher matcher = FILENAME.matcher(String.valueOf(
                response.headers().get("Content-Disposition")));
        if (!matcher.find()) {
            throw protocol("Labor quote PDF filename is unsafe.");
        }
        return matcher.group(1);
    }

    private static long stream(
            InputStream source,
            OutputStream destination,
            ActiveScope scope) throws LaborQuoteException {
        byte[] signature = readSignature(source);
        if (!Arrays.equals(signature, SIGNATURE)) {
            throw protocol("Labor quote document is not a PDF.");
        }
        write(destination, signature, signature.length);
        long total = signature.length;
        byte[] buffer = new byte[BUFFER_BYTES];
        while (true) {
            scope.requireActive();
            int read = read(source, buffer);
            if (read == -1) {
                flush(destination);
                return total;
            }
            if (total + read > MAXIMUM_BYTES) {
                throw protocol("Labor quote PDF exceeds the size limit.");
            }
            write(destination, buffer, read);
            total += read;
        }
    }

    private static byte[] readSignature(InputStream source) throws LaborQuoteException {
        byte[] signature = new byte[SIGNATURE.length];
        int offset = 0;
        while (offset < signature.length) {
            int read = read(source, signature, offset, signature.length - offset);
            if (read == -1) {
                throw protocol("Labor quote PDF is truncated.");
            }
            offset += read;
        }
        return signature;
    }

    private static int read(InputStream source, byte[] buffer) throws LaborQuoteException {
        return read(source, buffer, 0, buffer.length);
    }

    private static int read(InputStream source, byte[] buffer, int offset, int length)
            throws LaborQuoteException {
        try {
            return source.read(buffer, offset, length);
        } catch (IOException exception) {
            throw network(exception);
        }
    }

    private static void write(OutputStream destination, byte[] bytes, int length)
            throws LaborQuoteException {
        try {
            destination.write(bytes, 0, length);
        } catch (IOException exception) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.LOCAL_STORAGE,
                    "Labor quote PDF cannot be stored.",
                    exception);
        }
    }

    private static void flush(OutputStream destination) throws LaborQuoteException {
        try {
            destination.flush();
        } catch (IOException exception) {
            throw new LaborQuoteException(
                    LaborQuoteFailureKind.LOCAL_STORAGE,
                    "Labor quote PDF cannot be finalized.",
                    exception);
        }
    }

    private static LaborQuoteException protocol(String message) {
        return new LaborQuoteException(LaborQuoteFailureKind.PROTOCOL, message);
    }

    private static LaborQuoteException network(IOException exception) {
        return new LaborQuoteException(
                LaborQuoteFailureKind.NETWORK,
                "Labor quote PDF stream was interrupted.",
                exception);
    }

    @FunctionalInterface
    interface ActiveScope {
        void requireActive() throws LaborQuoteException;
    }
}
