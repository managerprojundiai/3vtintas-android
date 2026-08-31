package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class SystemConfigurationDtos {
    private static final int MINIMUM_REVISION = 1;
    private static final BigDecimal MAXIMUM_COMMISSION_RATE =
            new BigDecimal("100.00");
    private static final Pattern COMMISSION_RATE =
            Pattern.compile("^\\d{1,3}\\.\\d{2}$");

    private SystemConfigurationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsCommissionRate(String value) {
        if (value == null || !COMMISSION_RATE.matcher(value).matches()) {
            return false;
        }
        BigDecimal parsed = new BigDecimal(value);
        return parsed.compareTo(BigDecimal.ZERO) >= 0
                && parsed.compareTo(MAXIMUM_COMMISSION_RATE) <= 0;
    }

    public record Configuration(
            String storeName,
            String storePhone,
            String storeAddress,
            String laborCompanyName,
            String laborCompanyContact,
            String defaultCommissionRate,
            boolean autoApprovePainters,
            int revision,
            String updatedAt) {
        public Configuration {
            storeName = text(storeName, "Store name", 200);
            storePhone = text(storePhone, "Store phone", 40);
            storeAddress = text(storeAddress, "Store address", 500);
            laborCompanyName = text(laborCompanyName, "Labor company", 200);
            laborCompanyContact = text(
                    laborCompanyContact,
                    "Labor company contact",
                    200);
            defaultCommissionRate = rate(defaultCommissionRate);
            revision = requireRevision(revision);
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "System configuration update");
        }
    }

    public record UpdateRequest(
            String storeName,
            String storePhone,
            String storeAddress,
            String laborCompanyName,
            String laborCompanyContact,
            String defaultCommissionRate,
            boolean autoApprovePainters,
            int expectedRevision,
            boolean confirmed) {
        public UpdateRequest {
            storeName = text(storeName, "Store name", 200);
            storePhone = text(storePhone, "Store phone", 40);
            storeAddress = text(storeAddress, "Store address", 500);
            laborCompanyName = text(laborCompanyName, "Labor company", 200);
            laborCompanyContact = text(
                    laborCompanyContact,
                    "Labor company contact",
                    200);
            defaultCommissionRate = rate(defaultCommissionRate);
            expectedRevision = requireRevision(expectedRevision);
            if (!confirmed) {
                throw new IllegalArgumentException(
                        "System configuration update is not confirmed.");
            }
        }
    }

    public record Mutation(Configuration configuration, boolean changed) {
        public Mutation {
            if (configuration == null) {
                throw new IllegalArgumentException(
                        "System configuration mutation is invalid.");
            }
        }
    }

    private static String text(String value, String label, int limit) {
        if (value == null || value.length() > limit) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static String rate(String value) {
        if (!supportsCommissionRate(value)) {
            throw new IllegalArgumentException("Commission rate is invalid.");
        }
        return value;
    }

    private static int requireRevision(int value) {
        if (value < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "System configuration revision is invalid.");
        }
        return value;
    }
}
