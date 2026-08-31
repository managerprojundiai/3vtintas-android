package br.com.tresvtintas.mobile.core.quote;

import java.util.List;

public record MaterialQuotePricingContext(
        long organizationId,
        boolean enabled,
        String engineMode,
        int policyRevision,
        boolean selectionRequired,
        List<MaterialQuotePriceListOption> options) {
    private static final int REQUIRED_PRIMARY_COUNT = 1;
    private static final int MULTIPLE_OPTIONS_MINIMUM = 2;

    public MaterialQuotePricingContext {
        if (organizationId < 1 || policyRevision < 1
                || engineMode == null || engineMode.isBlank()
                || options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Pricing context is invalid.");
        }
        options = List.copyOf(options);
        if (options.stream().filter(MaterialQuotePriceListOption::primary).count()
                != REQUIRED_PRIMARY_COUNT) {
            throw new IllegalArgumentException("Pricing context requires one primary table.");
        }
        if (selectionRequired && options.size() < MULTIPLE_OPTIONS_MINIMUM) {
            throw new IllegalArgumentException("Required pricing selection has no alternatives.");
        }
    }

    public MaterialQuotePricingSelection selection(String versionPublicId) {
        MaterialQuotePriceListOption selected = options.stream()
                .filter(option -> option.versionPublicId().equals(versionPublicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected table is outside the current pricing context."));
        return new MaterialQuotePricingSelection(
                policyRevision,
                selectionRequired || !selected.primary()
                        ? java.util.Optional.of(selected.versionPublicId())
                        : java.util.Optional.empty());
    }
}
