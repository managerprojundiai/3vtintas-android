package br.com.tresvtintas.mobile.feature.laborquote;

import java.util.Optional;

@FunctionalInterface
public interface LaborQuoteRuntimeProvider {
    Optional<LaborQuoteFeatureRuntime> laborQuoteRuntime();
}
