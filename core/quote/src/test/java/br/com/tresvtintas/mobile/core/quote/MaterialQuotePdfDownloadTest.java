package br.com.tresvtintas.mobile.core.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class MaterialQuotePdfDownloadTest {
    @Test
    public void acceptsOnlyServerMaterialQuoteFilenames() {
        MaterialQuotePdfDownload download = new MaterialQuotePdfDownload(
                "orcamento-000501-material.pdf",
                1024);
        assertEquals(
                "The safe server filename must be preserved.",
                "orcamento-000501-material.pdf",
                download.filename());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialQuotePdfDownload("../../cliente.pdf", 1024));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialQuotePdfDownload(
                        "orcamento-000501-material.pdf",
                        4));
    }
}
