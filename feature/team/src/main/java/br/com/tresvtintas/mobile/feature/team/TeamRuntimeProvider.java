package br.com.tresvtintas.mobile.feature.team;

import java.util.Optional;

@FunctionalInterface
public interface TeamRuntimeProvider {
    Optional<TeamFeatureRuntime> teamRuntime();
}
