package br.com.tresvtintas.mobile.feature.systemconfiguration;

import java.util.Optional;

@FunctionalInterface
public interface SystemConfigurationRuntimeProvider {
    Optional<SystemConfigurationFeatureRuntime> systemConfigurationRuntime();
}
