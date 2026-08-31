package br.com.tresvtintas.mobile.feature.whatsappadmin;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

public final class WhatsAppQrPayloadTest {
    @Test
    public void decodesPlainAndDataUrlPayloads() {
        byte[] expected = "safe-qr".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(expected);

        assertArrayEquals(
                "Plain base64 must decode.",
                expected,
                WhatsAppQrPayload.decode(encoded));
        assertArrayEquals(
                "PNG data URL must decode without persisting it.",
                expected,
                WhatsAppQrPayload.decode("data:image/png;base64," + encoded));
    }

    @Test
    public void rejectsMalformedAndEmptyPayloads() {
        assertThrows(
                "Malformed base64 must fail closed.",
                IllegalArgumentException.class,
                () -> WhatsAppQrPayload.decode("%%%"));
        assertThrows(
                "Empty payload must fail closed.",
                IllegalArgumentException.class,
                () -> WhatsAppQrPayload.decode(""));
    }
}
