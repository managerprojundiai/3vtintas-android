package br.com.tresvtintas.mobile.feature.whatsappadmin;

import java.util.Optional;

@FunctionalInterface
public interface WhatsAppAdministrationRuntimeProvider {
    Optional<WhatsAppAdministrationFeatureRuntime>
            whatsAppAdministrationRuntime();
}
