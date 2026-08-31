package br.com.tresvtintas.mobile.feature.finance;

import android.content.Intent;
import br.com.tresvtintas.mobile.core.finance.FinanceExperience;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record FinanceRoute(
        FinanceExperience experience,
        OptionalLong organizationId,
        Optional<String> organizationName) {
    private static final String EXTRA_EXPERIENCE =
            "br.com.tresvtintas.mobile.finance.EXPERIENCE";
    private static final String EXTRA_ORGANIZATION_ID =
            "br.com.tresvtintas.mobile.finance.ORGANIZATION_ID";
    private static final String EXTRA_ORGANIZATION_NAME =
            "br.com.tresvtintas.mobile.finance.ORGANIZATION_NAME";

    public FinanceRoute {
        experience = Objects.requireNonNull(
                experience,
                "Finance experience is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        organizationName = organizationName == null
                ? Optional.empty()
                : organizationName.map(String::trim)
                        .filter(value -> !value.isEmpty());
        if ((organizationId.isPresent()
                        && organizationId.getAsLong() < 1)
                || organizationName
                        .filter(value -> value.length() > 200)
                        .isPresent()
                || (experience == FinanceExperience.PERSONAL
                        && (organizationId.isPresent()
                                || organizationName.isPresent()))
                || (organizationName.isPresent()
                        && organizationId.isEmpty())) {
            throw new IllegalArgumentException("Finance route is invalid.");
        }
    }

    public static FinanceRoute personal() {
        return new FinanceRoute(
                FinanceExperience.PERSONAL,
                OptionalLong.empty(),
                Optional.empty());
    }

    public static FinanceRoute corporateGlobal() {
        return new FinanceRoute(
                FinanceExperience.CORPORATE,
                OptionalLong.empty(),
                Optional.empty());
    }

    public static FinanceRoute corporate(
            long organizationId,
            String organizationName) {
        return new FinanceRoute(
                FinanceExperience.CORPORATE,
                OptionalLong.of(organizationId),
                Optional.ofNullable(organizationName));
    }

    public boolean canCreate() {
        return experience == FinanceExperience.PERSONAL
                || organizationId.isPresent();
    }

    Intent apply(Intent intent) {
        intent.putExtra(EXTRA_EXPERIENCE, experience.name());
        if (organizationId.isPresent()) {
            intent.putExtra(
                    EXTRA_ORGANIZATION_ID,
                    organizationId.getAsLong());
        }
        organizationName.ifPresent(value ->
                intent.putExtra(EXTRA_ORGANIZATION_NAME, value));
        return intent;
    }

    static FinanceRoute from(Intent intent) {
        if (intent == null) {
            return personal();
        }
        String rawExperience = intent.getStringExtra(EXTRA_EXPERIENCE);
        FinanceExperience value;
        try {
            value = rawExperience == null
                    ? FinanceExperience.PERSONAL
                    : FinanceExperience.valueOf(rawExperience);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Finance experience is invalid.",
                    exception);
        }
        long rawId = intent.getLongExtra(EXTRA_ORGANIZATION_ID, 0L);
        return new FinanceRoute(
                value,
                rawId > 0 ? OptionalLong.of(rawId) : OptionalLong.empty(),
                Optional.ofNullable(
                        intent.getStringExtra(EXTRA_ORGANIZATION_NAME)));
    }
}
