package br.com.tresvtintas.mobile.core.attendance;

import java.util.Optional;

@FunctionalInterface
public interface AttendanceRealtimeSource {
    interface Listener {
        void onEvent(AttendanceRealtimeEvent event);

        void onFailure(AttendanceException failure);

        void onClosed();
    }

    AttendanceRealtimeSubscription subscribe(
            Optional<String> lastEventId,
            Listener listener) throws AttendanceException;
}
