package br.com.tresvtintas.mobile.core.team;

@FunctionalInterface
public interface TeamRepository {
    TeamSnapshot load() throws TeamException;
}
