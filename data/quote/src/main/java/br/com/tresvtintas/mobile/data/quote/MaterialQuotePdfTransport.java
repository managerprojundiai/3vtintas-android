package br.com.tresvtintas.mobile.data.quote;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteFailureKind;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePdfDownload;
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

final class MaterialQuotePdfTransport {
    private static final int STREAM_BUFFER_BYTES = 16 * 1024;
    private static final long MAX_PDF_BYTES = 10L * 1024L * 1024L;
    private static final byte[] PDF_SIGNATURE = {37, 80, 68, 70, 45};
    private static final Pattern PDF_FILENAME = Pattern.compile(
            "(?i)(?:^|;)\\s*filename=\""
                    + "(orcamento-[0-9]{6,15}-material\\.pdf)\"(?:;|$)");

    private MaterialQuotePdfTransport() {
    }

    static MaterialQuotePdfDownload download(
            Response<ResponseBody> response,
            OutputStream destination,
            ActiveScope activeScope) throws MaterialQuoteException {
        try (ResponseBody body = requireBody(response);
             InputStream source = body.byteStream()) {
            validateHeaders(response, body);
            String filename = filename(response);
            long written = stream(source, destination, activeScope);
            return new MaterialQuotePdfDownload(filename, written);
        } catch (IOException exception) {
            throw network(exception);
        }
    }

    private static ResponseBody requireBody(Response<ResponseBody> response)
            throws MaterialQuoteException {
        ResponseBody body = response.body();
        if (body == null) {
            throw protocol("The quote response did not contain a body.");
        }
        return body;
    }

    private static void validateHeaders(
            Response<ResponseBody> response,
            ResponseBody body) throws MaterialQuoteException {
        MediaType type = body.contentType();
        long length = body.contentLength();
        String cacheControl = response.headers().get("Cache-Control");
        String contentOptions = response.headers().get(
                "X-Content-Type-Options");
        if (type == null
                || !"application".equalsIgnoreCase(type.type())
                || !"pdf".equalsIgnoreCase(type.subtype())
                || (length != -1
                        && (length < PDF_SIGNATURE.length
                                || length > MAX_PDF_BYTES))
                || cacheControl == null
                || !cacheControl.toLowerCase(Locale.ROOT).contains("no-store")
                || !"nosniff".equalsIgnoreCase(contentOptions)) {
            throw protocol("The quote PDF headers are invalid.");
        }
    }

    private static String filename(Response<?> response)
            throws MaterialQuoteException {
        String disposition = response.headers().get("Content-Disposition");
        Matcher matcher = PDF_FILENAME.matcher(
                disposition == null ? "" : disposition);
        if (!matcher.find()) {
            throw protocol("The quote PDF filename is invalid.");
        }
        return matcher.group(1);
    }

    private static long stream(
            InputStream source,
            OutputStream destination,
            ActiveScope activeScope) throws MaterialQuoteException {
        byte[] signature = readSignature(source);
        if (!Arrays.equals(signature, PDF_SIGNATURE)) {
            throw protocol("The quote document is not a PDF.");
        }
        write(destination, signature, signature.length);
        long total = signature.length;
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        while (true) {
            activeScope.requireActive();
            int read = read(source, buffer);
            if (read == -1) {
                flush(destination);
                return total;
            }
            if (total + read > MAX_PDF_BYTES) {
                throw protocol("The quote PDF exceeds the allowed size.");
            }
            write(destination, buffer, read);
            total += read;
        }
    }

    private static byte[] readSignature(InputStream source)
            throws MaterialQuoteException {
        byte[] signature = new byte[PDF_SIGNATURE.length];
        int offset = 0;
        while (offset < signature.length) {
            int read;
            try {
                read = source.read(signature, offset, signature.length - offset);
            } catch (IOException exception) {
                throw network(exception);
            }
            if (read == -1) {
                throw protocol("The quote PDF is truncated.");
            }
            offset += read;
        }
        return signature;
    }

    private static int read(InputStream source, byte[] buffer)
            throws MaterialQuoteException {
        try {
            return source.read(buffer);
        } catch (IOException exception) {
            throw network(exception);
        }
    }

    private static void write(
            OutputStream destination,
            byte[] bytes,
            int length) throws MaterialQuoteException {
        try {
            destination.write(bytes, 0, length);
        } catch (IOException exception) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.LOCAL_STORAGE,
                    "The quote PDF could not be stored temporarily.",
                    exception);
        }
    }

    private static void flush(OutputStream destination)
            throws MaterialQuoteException {
        try {
            destination.flush();
        } catch (IOException exception) {
            throw new MaterialQuoteException(
                    MaterialQuoteFailureKind.LOCAL_STORAGE,
                    "The quote PDF could not be finalized.",
                    exception);
        }
    }

    private static MaterialQuoteException protocol(String message) {
        return new MaterialQuoteException(
                MaterialQuoteFailureKind.PROTOCOL,
                message);
    }

    private static MaterialQuoteException network(IOException exception) {
        return new MaterialQuoteException(
                MaterialQuoteFailureKind.NETWORK,
                "The quote PDF stream was interrupted.",
                exception);
    }

    @FunctionalInterface
    interface ActiveScope {
        void requireActive() throws MaterialQuoteException;
    }
}
