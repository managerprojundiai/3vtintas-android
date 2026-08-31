package br.com.tresvtintas.mobile.core.catalog;

import java.util.List;
import java.util.Optional;

public record CatalogPricingContext(
        long organizationId,
        int policyRevision,
        boolean selectionRequired,
        List<CatalogPriceListOption> options,
        Optional<String> selectedVersionPublicId) {
    private static final long REQUIRED_PRIMARY_COUNT = 1L;
    private static final int MULTIPLE_OPTIONS_MINIMUM = 2;

    public CatalogPricingContext {
        if (organizationId < 1 || policyRevision < 1 || options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Catalog pricing context is invalid.");
        }
        options = List.copyOf(options);
        selectedVersionPublicId = selectedVersionPublicId == null
                ? Optional.empty()
                : selectedVersionPublicId;
        if (options.stream().filter(CatalogPriceListOption::primary).count()
                != REQUIRED_PRIMARY_COUNT) {
            throw new IllegalArgumentException("Catalog pricing requires one primary table.");
        }
        if (selectionRequired && options.size() < MULTIPLE_OPTIONS_MINIMUM) {
            throw new IllegalArgumentException("Required table selection has no alternatives.");
        }
        if (selectedVersionPublicId.isPresent()) {
            option(selectedVersionPublicId.orElseThrow(), options);
        }
    }

    public CatalogPricingContext select(String versionPublicId) {
        CatalogPriceListOption selected = option(versionPublicId, options);
        return new CatalogPricingContext(
                organizationId,
                policyRevision,
                selectionRequired,
                options,
                Optional.of(selected.versionPublicId()));
    }

    public boolean needsSelection() {
        return selectionRequired && selectedVersionPublicId.isEmpty();
    }

    public CatalogPriceListOption primary() {
        return options.stream()
                .filter(CatalogPriceListOption::primary)
                .findFirst()
                .orElseThrow();
    }

    private static CatalogPriceListOption option(
            String versionPublicId,
            List<CatalogPriceListOption> options) {
        if (versionPublicId == null) {
            throw new IllegalArgumentException("Catalog table selection is required.");
        }
        return options.stream()
                .filter(option -> option.versionPublicId().equals(versionPublicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected table is outside the catalog pricing context."));
    }
}
