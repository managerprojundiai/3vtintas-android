package br.com.tresvtintas.mobile.feature.finance;

import java.util.Optional;

@FunctionalInterface
public interface FinanceRuntimeProvider {
    Optional<FinanceFeatureRuntime> financeRuntime(FinanceRoute route);
}
