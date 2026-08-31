package br.com.tresvtintas.mobile.core.team;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record TeamMember(
        long painterId,
        String name,
        Optional<String> company,
        BigDecimal commissionRate,
        Status status,
        int totalQuotes,
        int totalOrders,
        int conversionBasisPoints,
        BigDecimal totalSales,
        Optional<TeamRegion> topRegion) {
    public TeamMember {
        company = company == null ? Optional.empty() : company;
        topRegion = topRegion == null ? Optional.empty() : topRegion;
        Objects.requireNonNull(
                commissionRate,
                "Team commission rate is required.");
        Objects.requireNonNull(status, "Team member status is required.");
        Objects.requireNonNull(totalSales, "Team sales total is required.");
        if (painterId < 1
                || name == null
                || name.isBlank()
                || commissionRate.signum() < 0
                || totalQuotes < 0
                || totalOrders < 0
                || conversionBasisPoints < 0
                || conversionBasisPoints > 10000
                || totalSales.signum() < 0) {
            throw new IllegalArgumentException("Team member is invalid.");
        }
    }

    public enum Status {
        PENDING,
        ACTIVE,
        BLOCKED
    }
}
