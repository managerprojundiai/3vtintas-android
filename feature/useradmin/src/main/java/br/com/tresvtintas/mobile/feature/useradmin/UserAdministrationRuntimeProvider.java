package br.com.tresvtintas.mobile.feature.useradmin;

import java.util.Optional;

@FunctionalInterface
public interface UserAdministrationRuntimeProvider {
    Optional<UserAdministrationFeatureRuntime> userAdministrationRuntime();
}
