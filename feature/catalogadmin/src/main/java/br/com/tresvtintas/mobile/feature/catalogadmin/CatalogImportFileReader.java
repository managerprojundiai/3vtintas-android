package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class CatalogImportFileReader {
    private static final int BUFFER_BYTES = 16 * 1024;
    private static final int END_OF_STREAM = -1;
    private static final int HEXADECIMAL_RADIX = 16;
    private static final int HIGH_NIBBLE_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final String CSV_MEDIA_TYPE = "text/csv";
    private static final String XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private CatalogImportFileReader() {
        throw new AssertionError("No instances.");
    }

    static SourceFile read(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String fileName = displayName(resolver, uri);
        String extension = extension(fileName);
        String mediaType = switch (extension) {
            case "csv" -> CSV_MEDIA_TYPE;
            case "xlsx" -> XLSX_MEDIA_TYPE;
            default -> throw new IOException("CATALOG_IMPORT_FILE_TYPE");
        };
        byte[] bytes;
        try (InputStream source = resolver.openInputStream(uri)) {
            if (source == null) {
                throw new IOException("CATALOG_IMPORT_FILE_UNAVAILABLE");
            }
            bytes = bounded(source);
        }
        return new SourceFile(
                fileName,
                mediaType,
                sha256(bytes),
                bytes);
    }

    private static byte[] bounded(InputStream source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        int total = 0;
        while (true) {
            int read = source.read(buffer);
            if (read == END_OF_STREAM) {
                break;
            }
            total += read;
            if (total > CatalogImportModels.MAXIMUM_FILE_BYTES) {
                throw new IOException("CATALOG_IMPORT_FILE_TOO_LARGE");
            }
            output.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new IOException("CATALOG_IMPORT_FILE_EMPTY");
        }
        return output.toByteArray();
    }

    private static String displayName(ContentResolver resolver, Uri uri)
            throws IOException {
        try (Cursor cursor = resolver.query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor != null
                    && cursor.moveToFirst()
                    && !cursor.isNull(0)) {
                String value = cursor.getString(0);
                if (value != null && !value.isBlank() && value.length() <= 255) {
                    return value;
                }
            }
        }
        throw new IOException("CATALOG_IMPORT_FILE_NAME");
    }

    private static String extension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator < 0
                ? ""
                : fileName.substring(separator + 1)
                        .toLowerCase(Locale.ROOT);
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(bytes);
            char[] encoded = new char[digest.length * 2];
            for (int index = 0; index < digest.length; index++) {
                int value = Byte.toUnsignedInt(digest[index]);
                encoded[index * 2] = Character.forDigit(
                        value >>> HIGH_NIBBLE_SHIFT,
                        HEXADECIMAL_RADIX);
                encoded[index * 2 + 1] = Character.forDigit(
                        value & LOW_NIBBLE_MASK,
                        HEXADECIMAL_RADIX);
            }
            return new String(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 is required by the Android runtime.",
                    exception);
        }
    }
}
