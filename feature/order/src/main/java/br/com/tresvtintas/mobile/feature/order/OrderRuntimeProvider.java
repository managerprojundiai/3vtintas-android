package br.com.tresvtintas.mobile.feature.order;

import java.util.Optional;

@FunctionalInterface
public interface OrderRuntimeProvider {
    Optional<OrderFeatureRuntime> orderRuntime();
}
