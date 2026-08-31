package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintConfiguration;
import java.util.List;
import org.junit.Test;

public final class MaterialQuoteTintOptionsTest {
    private static final String LKC = "LKC";
    private static final String RUBBERIZED = "Emborrachada";
    private static final String MATTE = "Fosco";
    private static final String LARGE_PACKAGE = "18 L";
    private static final List<MaterialQuoteTintConfiguration> CONFIGURATIONS = List.of(
            configuration(LKC, RUBBERIZED, MATTE, LARGE_PACKAGE),
            configuration(LKC, RUBBERIZED, MATTE, "3,6 L"),
            configuration(LKC, "Esmalte Sintético", "Brilhante", "3,6 L"),
            configuration("CORIMO", "Coralux", MATTE, LARGE_PACKAGE));

    @Test
    public void narrowsTheSalesConfigurationWithoutInventingValues() {
        assertEquals(
                "Source systems must be stable and sorted.",
                List.of("CORIMO", LKC),
                MaterialQuoteTintOptions.sources(CONFIGURATIONS));
        assertEquals(
                "Only lines from the selected source are offered.",
                List.of(RUBBERIZED, "Esmalte Sintético"),
                MaterialQuoteTintOptions.lines(CONFIGURATIONS, LKC));
        assertEquals(
                "Only matching packages are offered.",
                List.of(LARGE_PACKAGE, "3,6 L"),
                MaterialQuoteTintOptions.packages(
                        CONFIGURATIONS,
                        LKC,
                        RUBBERIZED,
                        MATTE));
    }

    @Test
    public void rejectsAConfigurationThatTheServerDidNotReturn() {
        assertThrows(
                "A UI combination absent from the server must fail closed.",
                IllegalArgumentException.class,
                () -> MaterialQuoteTintOptions.configuration(
                        CONFIGURATIONS,
                        LKC,
                        RUBBERIZED,
                        "Acetinado",
                        LARGE_PACKAGE));
    }

    private static MaterialQuoteTintConfiguration configuration(
            String source,
            String line,
            String finish,
            String packageName) {
        return new MaterialQuoteTintConfiguration(
                source,
                line,
                finish,
                packageName);
    }
}
