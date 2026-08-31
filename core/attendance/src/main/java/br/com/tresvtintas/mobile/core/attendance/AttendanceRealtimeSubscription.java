package br.com.tresvtintas.mobile.core.attendance;

@FunctionalInterface
public interface AttendanceRealtimeSubscription extends AutoCloseable {
    @Override
    void close();
}
