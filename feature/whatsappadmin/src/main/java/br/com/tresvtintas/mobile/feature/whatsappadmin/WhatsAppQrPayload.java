package br.com.tresvtintas.mobile.feature.whatsappadmin;

import java.util.Base64;

final class WhatsAppQrPayload {
    private static final int MAXIMUM_DECODED_BYTES = 600_000;
    private static final String BASE64_MARKER = ";base64,";

    private WhatsAppQrPayload() {
        throw new AssertionError("No instances.");
    }

    static byte[] decode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QR payload is required.");
        }
        String normalized = value.strip();
        int marker = normalized.indexOf(BASE64_MARKER);
        if (normalized.startsWith("data:")) {
            if (marker < 0) {
                throw new IllegalArgumentException("QR data URL is invalid.");
            }
            normalized = normalized.substring(marker + BASE64_MARKER.length());
        }
        byte[] result;
        try {
            result = Base64.getMimeDecoder().decode(normalized);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("QR base64 is invalid.", failure);
        }
        if (result.length == 0 || result.length > MAXIMUM_DECODED_BYTES) {
            throw new IllegalArgumentException("QR payload size is invalid.");
        }
        return result;
    }
}
