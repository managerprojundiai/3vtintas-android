package br.com.tresvtintas.mobile.feature.systemconfiguration;

import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationController;
import java.util.Objects;

public record SystemConfigurationFeatureRuntime(
        SystemConfigurationController controller) {
    public SystemConfigurationFeatureRuntime {
        Objects.requireNonNull(controller, "Configuration controller is required.");
    }
}
