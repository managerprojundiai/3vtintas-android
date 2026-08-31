package br.com.tresvtintas.mobile.feature.customer;

import java.util.Optional;

@FunctionalInterface
public interface CustomerRuntimeProvider {
    Optional<CustomerFeatureRuntime> customerRuntime();
}
