package br.com.tresvtintas.mobile.feature.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.junit.Test;

public final class TeamTextTest {
    @Test
    public void basisPointsUseBrazilianPercentage() {
        assertEquals(
                "Conversion must retain two decimal places when needed.",
                "50%",
                TeamText.percent(5000));
    }

    @Test
    public void moneyUsesBrazilianCurrency() {
        String formatted = TeamText.money(new BigDecimal("301.25"));

        assertTrue(
                "Sales must be rendered as Brazilian currency.",
                formatted.contains("301,25"));
    }
}
