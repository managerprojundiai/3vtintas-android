package br.com.tresvtintas.mobile.core.systemconfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public final class SystemConfigurationModels {
    private static final int MINIMUM_REVISION = 1;
    private static final Pattern RATE = Pattern.compile("^\\d{1,3}\\.\\d{2}$");

    private SystemConfigurationModels() {
        throw new AssertionError("No instances.");
    }

    public record Values(
            String storeName,
            String storePhone,
            String storeAddress,
            String laborCompanyName,
            String laborCompanyContact,
            String defaultCommissionRate,
            boolean autoApprovePainters) {
        public Values {
            storeName = text(storeName, 200, "Store name");
            storePhone = text(storePhone, 40, "Store phone");
            storeAddress = text(storeAddress, 500, "Store address");
            laborCompanyName = text(laborCompanyName, 200, "Labor company");
            laborCompanyContact = text(
                    laborCompanyContact,
                    200,
                    "Labor contact");
            defaultCommissionRate = rate(defaultCommissionRate);
        }
    }

    public record Snapshot(Values values, int revision, Instant updatedAt) {
        public Snapshot {
            Objects.requireNonNull(values, "Configuration values are required.");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException(
                        "Configuration revision is invalid.");
            }
            Objects.requireNonNull(updatedAt, "Configuration update is required.");
        }
    }

    public record Mutation(
            Snapshot configuration,
            boolean changed,
            boolean replayed) {
        public Mutation {
            Objects.requireNonNull(
                    configuration,
                    "Configuration mutation is required.");
        }
    }

    public static boolean sameValues(Values first, Values second) {
        return Objects.equals(first, second);
    }

    private static String text(String value, int limit, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > limit) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return normalized;
    }

    private static String rate(String value) {
        if (value == null || !RATE.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Commission rate is invalid.");
        }
        BigDecimal parsed = new BigDecimal(value.trim());
        if (parsed.compareTo(BigDecimal.ZERO) < 0
                || parsed.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Commission rate is invalid.");
        }
        return value.trim();
    }
}
