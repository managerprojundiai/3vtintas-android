package br.com.tresvtintas.mobile.core.appointment;

@FunctionalInterface
public interface AgendaStateListener {
    void onAgendaStateChanged(AgendaState state);
}
