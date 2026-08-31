package br.com.tresvtintas.mobile.feature.accountaccess;

import java.util.Optional;

@FunctionalInterface
public interface ManagedAccountAccessRuntimeProvider {
    Optional<ManagedAccountAccessFeatureRuntime>
            managedAccountAccessRuntime();
}
