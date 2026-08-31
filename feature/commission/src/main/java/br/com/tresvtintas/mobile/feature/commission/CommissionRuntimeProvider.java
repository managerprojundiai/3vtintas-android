package br.com.tresvtintas.mobile.feature.commission;

import java.util.Optional;

@FunctionalInterface
public interface CommissionRuntimeProvider {
    Optional<CommissionFeatureRuntime> commissionRuntime();
}
