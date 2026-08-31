package br.com.tresvtintas.mobile.core.laborquote;

import java.util.regex.Pattern;

public record LaborQuotePdfDownload(String filename, long bytesWritten) {
    private static final Pattern SAFE_NAME = Pattern.compile(
            "orcamento-[0-9]{6,15}-labor\\.pdf");

    public LaborQuotePdfDownload {
        if (filename == null || !SAFE_NAME.matcher(filename).matches() || bytesWritten < 5) {
            throw new IllegalArgumentException("Labor quote PDF is invalid.");
        }
    }
}
