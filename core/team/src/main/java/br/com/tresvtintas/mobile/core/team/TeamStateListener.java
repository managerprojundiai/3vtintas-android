package br.com.tresvtintas.mobile.core.team;

@FunctionalInterface
public interface TeamStateListener {
    void onTeamStateChanged(TeamState state);
}
