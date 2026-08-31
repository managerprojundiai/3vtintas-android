package br.com.tresvtintas.mobile.core.bootstrap;

import java.util.Optional;

public record BootstrapState(
        Phase phase,
        Optional<BootstrapSnapshot> snapshot,
        Optional<BootstrapFailureKind> failure,
        Optional<String> requestId) {

    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR
    }

    public BootstrapState {
        if (phase == null) {
            throw new IllegalArgumentException("Bootstrap phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.READY && snapshot.isEmpty()) {
            throw new IllegalArgumentException("Ready bootstrap state requires a snapshot.");
        }
        if (phase == Phase.ERROR && failure.isEmpty()) {
            throw new IllegalArgumentException("Error bootstrap state requires a failure.");
        }
    }

    public static BootstrapState empty() {
        return new BootstrapState(
                Phase.EMPTY, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static BootstrapState loading() {
        return new BootstrapState(
                Phase.LOADING, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static BootstrapState ready(BootstrapSnapshot snapshot) {
        return new BootstrapState(
                Phase.READY, Optional.of(snapshot), Optional.empty(), Optional.empty());
    }

    public static BootstrapState error(BootstrapException exception) {
        return new BootstrapState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }
}
