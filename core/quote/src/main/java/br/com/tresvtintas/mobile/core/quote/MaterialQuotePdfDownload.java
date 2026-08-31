package br.com.tresvtintas.mobile.core.quote;

import java.util.regex.Pattern;

public record MaterialQuotePdfDownload(String filename, long bytesWritten) {
    private static final int MINIMUM_PDF_BYTES = 5;
    private static final Pattern SAFE_PDF_NAME = Pattern.compile(
            "orcamento-[0-9]{6,15}-material\\.pdf");

    public MaterialQuotePdfDownload {
        if (filename == null || !SAFE_PDF_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("PDF filename is invalid.");
        }
        if (bytesWritten < MINIMUM_PDF_BYTES) {
            throw new IllegalArgumentException("PDF length is invalid.");
        }
    }
}
