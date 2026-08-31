package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record TeamResponseDto(
        String generatedAt,
        Context context,
        Summary summary,
        List<Member> members,
        List<Region> regions) {
    private static final int MAXIMUM_MEMBERS = 500;
    private static final int MAXIMUM_REGIONS = 5;
    private static final Set<String> VISIBILITIES = Set.of("team", "all");
    private static final Set<String> STATUSES =
            Set.of("pending", "active", "blocked");
    private static final Pattern MONEY =
            Pattern.compile("^\\d{1,18}\\.\\d{2}$");
    private static final Pattern RATE =
            Pattern.compile("^\\d{1,3}\\.\\d{2}$");

    public TeamResponseDto {
        generatedAt = DtoValidation.requireInstant(
                generatedAt,
                "Team generation time");
        if (context == null
                || summary == null
                || members == null
                || members.size() > MAXIMUM_MEMBERS
                || regions == null
                || regions.size() > MAXIMUM_REGIONS
                || summary.teamPainters() != members.size()) {
            throw new IllegalArgumentException("Team response is invalid.");
        }
        members = List.copyOf(members);
        regions = List.copyOf(regions);
    }

    public record Context(
            String visibility,
            Organization organization) {
        public Context {
            if (visibility == null || !VISIBILITIES.contains(visibility)) {
                throw new IllegalArgumentException(
                        "Team visibility is invalid.");
            }
            if ("team".equals(visibility) && organization == null) {
                throw new IllegalArgumentException(
                        "Team organization is required.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Team organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Team organization name",
                    200);
        }
    }

    public record Summary(
            int teamPainters,
            int totalOrders,
            int totalQuotes,
            String totalSales,
            Region topRegion) {
        public Summary {
            nonNegative(teamPainters, totalOrders, totalQuotes);
            totalSales = money(totalSales, "Team sales total");
        }
    }

    public record Member(
            long painterId,
            String name,
            String company,
            String commissionRate,
            String status,
            int totalQuotes,
            int totalOrders,
            int conversionBasisPoints,
            String totalSales,
            Region topRegion) {
        public Member {
            painterId = DtoValidation.requirePositive(
                    painterId,
                    "Team painter ID");
            name = DtoValidation.requireText(name, "Team member name", 200);
            company = DtoValidation.optionalText(
                    company,
                    "Team member company",
                    200);
            if (commissionRate == null
                    || !RATE.matcher(commissionRate).matches()
                    || status == null
                    || !STATUSES.contains(status)
                    || conversionBasisPoints < 0
                    || conversionBasisPoints > 10000) {
                throw new IllegalArgumentException("Team member is invalid.");
            }
            nonNegative(totalQuotes, totalOrders);
            totalSales = money(totalSales, "Team member sales total");
        }
    }

    public record Region(String label, int count) {
        public Region {
            label = DtoValidation.requireText(label, "Team region", 163);
            nonNegative(count);
        }
    }

    private static String money(String value, String fieldName) {
        if (value == null || !MONEY.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return value;
    }

    private static void nonNegative(int... values) {
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Team count is invalid.");
            }
        }
    }
}
