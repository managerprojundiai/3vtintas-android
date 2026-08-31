package br.com.tresvtintas.mobile.feature.delivery;

import java.util.Optional;

@FunctionalInterface
public interface DeliveryRuntimeProvider {
    Optional<DeliveryFeatureRuntime> deliveryRuntime();
}
