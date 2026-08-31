package br.com.tresvtintas.mobile.core.team;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record TeamSummary(
        int teamPainters,
        int totalOrders,
        int totalQuotes,
        BigDecimal totalSales,
        Optional<TeamRegion> topRegion) {
    public TeamSummary {
        topRegion = topRegion == null ? Optional.empty() : topRegion;
        Objects.requireNonNull(totalSales, "Team sales total is required.");
        if (teamPainters < 0
                || totalOrders < 0
                || totalQuotes < 0
                || totalSales.signum() < 0) {
            throw new IllegalArgumentException("Team summary is invalid.");
        }
    }
}
