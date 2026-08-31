package br.com.tresvtintas.mobile.core.systemconfiguration;

import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import java.util.Objects;
import java.util.Optional;

public record SystemConfigurationState(
        Phase phase,
        Optional<Snapshot> configuration,
        Optional<SystemConfigurationException> failure,
        boolean changed,
        boolean replayed) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        SAVING,
        CLOSED
    }

    public SystemConfigurationState {
        Objects.requireNonNull(phase, "Configuration phase is required.");
        Objects.requireNonNull(
                configuration,
                "Configuration snapshot container is required.");
        Objects.requireNonNull(failure, "Configuration failure is required.");
    }

    public static SystemConfigurationState empty() {
        return new SystemConfigurationState(
                Phase.EMPTY,
                Optional.empty(),
                Optional.empty(),
                false,
                false);
    }

    public static SystemConfigurationState loading() {
        return new SystemConfigurationState(
                Phase.LOADING,
                Optional.empty(),
                Optional.empty(),
                false,
                false);
    }

    public static SystemConfigurationState ready(Snapshot snapshot) {
        return ready(snapshot, Optional.empty(), false, false);
    }

    public static SystemConfigurationState ready(
            Snapshot snapshot,
            Optional<SystemConfigurationException> failure,
            boolean changed,
            boolean replayed) {
        return new SystemConfigurationState(
                Phase.READY,
                Optional.of(Objects.requireNonNull(snapshot)),
                Objects.requireNonNull(failure),
                changed,
                replayed);
    }

    public static SystemConfigurationState saving(Snapshot snapshot) {
        return new SystemConfigurationState(
                Phase.SAVING,
                Optional.of(Objects.requireNonNull(snapshot)),
                Optional.empty(),
                false,
                false);
    }

    public static SystemConfigurationState closed() {
        return new SystemConfigurationState(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                false,
                false);
    }
}
