package br.com.tresvtintas.mobile.feature.quote;

import java.util.Optional;

@FunctionalInterface
public interface MaterialQuoteRuntimeProvider {
    Optional<MaterialQuoteFeatureRuntime> materialQuoteRuntime();
}
