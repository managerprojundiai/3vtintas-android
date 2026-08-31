package br.com.tresvtintas.mobile.feature.painteradmin;

import java.util.Optional;

@FunctionalInterface
public interface PainterAdministrationRuntimeProvider {
    Optional<PainterAdministrationFeatureRuntime>
            painterAdministrationRuntime();
}
