package br.com.tresvtintas.mobile.core.whatsappadmin;

import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import java.util.Objects;
import java.util.Optional;

public record WhatsAppAdministrationState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<WhatsAppAdministrationException> failure,
        Optional<ActionResult> lastAction,
        Optional<EphemeralQr> ephemeralQr) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        MUTATING,
        QR_LOADING,
        CLOSED
    }

    public WhatsAppAdministrationState {
        Objects.requireNonNull(phase, "WhatsApp phase is required.");
        Objects.requireNonNull(snapshot, "Snapshot container is required.");
        Objects.requireNonNull(failure, "Failure container is required.");
        Objects.requireNonNull(lastAction, "Action container is required.");
        Objects.requireNonNull(ephemeralQr, "QR container is required.");
    }

    public static WhatsAppAdministrationState empty() {
        return state(Phase.EMPTY, Optional.empty());
    }

    public static WhatsAppAdministrationState loading(Optional<Snapshot> snapshot) {
        return state(Phase.LOADING, snapshot);
    }

    public static WhatsAppAdministrationState ready(Snapshot snapshot) {
        return state(Phase.READY, Optional.of(snapshot));
    }

    public static WhatsAppAdministrationState mutating(Snapshot snapshot) {
        return state(Phase.MUTATING, Optional.of(snapshot));
    }

    public static WhatsAppAdministrationState qrLoading(Snapshot snapshot) {
        return state(Phase.QR_LOADING, Optional.of(snapshot));
    }

    public static WhatsAppAdministrationState closed() {
        return state(Phase.CLOSED, Optional.empty());
    }

    public WhatsAppAdministrationState withFailure(
            WhatsAppAdministrationException value) {
        return new WhatsAppAdministrationState(
                Phase.READY,
                snapshot,
                Optional.of(Objects.requireNonNull(value)),
                Optional.empty(),
                Optional.empty());
    }

    public WhatsAppAdministrationState withAction(
            Snapshot fresh,
            ActionResult value) {
        return new WhatsAppAdministrationState(
                Phase.READY,
                Optional.of(Objects.requireNonNull(fresh)),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(value)),
                Optional.empty());
    }

    public WhatsAppAdministrationState withQr(EphemeralQr value) {
        return new WhatsAppAdministrationState(
                Phase.READY,
                snapshot,
                Optional.empty(),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(value)));
    }

    public WhatsAppAdministrationState withoutQr() {
        return new WhatsAppAdministrationState(
                phase,
                snapshot,
                failure,
                lastAction,
                Optional.empty());
    }

    public WhatsAppAdministrationState withoutTransient() {
        return new WhatsAppAdministrationState(
                phase,
                snapshot,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static WhatsAppAdministrationState state(
            Phase phase,
            Optional<Snapshot> snapshot) {
        return new WhatsAppAdministrationState(
                phase,
                snapshot,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
