package br.com.tresvtintas.mobile.feature.quote;

import br.com.tresvtintas.mobile.core.quote.MaterialQuotePriceListOption;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingContext;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSelection;
import java.util.Optional;

final class MaterialQuotePricingState {
    private final Optional<MaterialQuotePricingContext> context;
    private final Optional<String> selectedVersionPublicId;

    private MaterialQuotePricingState(
            Optional<MaterialQuotePricingContext> context,
            Optional<String> selectedVersionPublicId) {
        this.context = context;
        this.selectedVersionPublicId = selectedVersionPublicId;
    }

    static MaterialQuotePricingState legacy() {
        return new MaterialQuotePricingState(Optional.empty(), Optional.empty());
    }

    static MaterialQuotePricingState from(MaterialQuotePricingContext context) {
        if (!context.enabled()) {
            return legacy();
        }
        Optional<String> initial = context.selectionRequired()
                ? Optional.empty()
                : context.options().stream()
                        .filter(MaterialQuotePriceListOption::primary)
                        .map(MaterialQuotePriceListOption::versionPublicId)
                        .findFirst();
        return new MaterialQuotePricingState(Optional.of(context), initial);
    }

    MaterialQuotePricingState select(String versionPublicId) {
        MaterialQuotePricingContext available = context.orElseThrow();
        available.selection(versionPublicId);
        return new MaterialQuotePricingState(context, Optional.of(versionPublicId));
    }

    boolean visible() {
        return context.isPresent();
    }

    boolean ready() {
        return context.isEmpty() || selectedVersionPublicId.isPresent();
    }

    boolean selectionRequired() {
        return context.map(MaterialQuotePricingContext::selectionRequired)
                .orElse(false);
    }

    boolean selectable() {
        return context.map(value -> value.options().size() > 1).orElse(false);
    }

    boolean supports(String versionPublicId) {
        return context.map(value -> value.options().stream()
                .anyMatch(option -> option.versionPublicId().equals(versionPublicId)))
                .orElse(false);
    }

    Optional<MaterialQuotePriceListOption> selectedOption() {
        return context.flatMap(value -> value.options().stream()
                .filter(option -> selectedVersionPublicId
                        .map(option.versionPublicId()::equals)
                        .orElse(false))
                .findFirst());
    }

    Optional<MaterialQuotePricingSelection> claim() {
        if (context.isEmpty()) {
            return Optional.empty();
        }
        return selectedVersionPublicId.map(context.orElseThrow()::selection);
    }

    MaterialQuotePricingContext context() {
        return context.orElseThrow();
    }
}
