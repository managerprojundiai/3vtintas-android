package br.com.tresvtintas.mobile.core.dashboard;

@FunctionalInterface
public interface DashboardRepository {
    DashboardSnapshot load() throws DashboardException;
}
