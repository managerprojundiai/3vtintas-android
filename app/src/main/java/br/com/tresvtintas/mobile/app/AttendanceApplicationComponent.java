package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeCoordinator;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.data.attendance.AttendanceAccountScope;
import br.com.tresvtintas.mobile.data.attendance.RemoteAttendanceEventSource;
import br.com.tresvtintas.mobile.data.attendance.RemoteAttendanceRepository;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceFeatureRuntime;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Owns the sensitive attendance session independently from the application root. Conversation
 * previews and message history stay in feature memory and are released whenever authorization
 * changes or the user leaves the authenticated shell.
 */
final class AttendanceApplicationComponent implements AutoCloseable {
    private final MobileApi protectedApi;
    private final MobileNetworkClient protectedClient;
    private final ExecutorService worker;
    private final ScheduledExecutorService realtimeWorker;
    private final Executor callbackExecutor;
    private Optional<Session> session = Optional.empty();

    AttendanceApplicationComponent(
            MobileNetworkClient protectedClient,
            Executor callbackExecutor) {
        if (protectedClient == null) {
            throw new IllegalArgumentException(
                    "Protected attendance client is required.");
        }
        this.protectedClient = protectedClient;
        protectedApi = protectedClient.api();
        this.callbackExecutor = java.util.Objects.requireNonNull(
                callbackExecutor,
                "Attendance callback executor is required.");
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "3v-attendance-worker");
            thread.setDaemon(false);
            return thread;
        });
        realtimeWorker = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-attendance-realtime");
                    thread.setDaemon(false);
                    return thread;
                });
    }

    synchronized void activate(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !canRead(access)
                || worker.isShutdown()) {
            deactivate();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        AttendanceAccountScope scope = new AttendanceAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                organizationId);
        boolean assignedOnly = false;
        boolean canReply = access.bootstrap().authorization().has(
                Capability.ATTENDANCE_REPLY);
        boolean canManage = access.bootstrap().authorization().has(
                Capability.ATTENDANCE_MANAGE);
        if (session.filter(value ->
                        value.scope().equals(scope)
                                && value.runtime().assignedOnly()
                                        == assignedOnly
                                && value.runtime().canReply()
                                        == canReply
                                && value.runtime().canManage()
                                        == canManage)
                .isPresent()) {
            return;
        }
        deactivate();
        RemoteAttendanceRepository repository =
                new RemoteAttendanceRepository(scope, protectedApi);
        RemoteAttendanceEventSource realtimeSource =
                new RemoteAttendanceEventSource(
                        scope,
                        protectedClient);
        AttendanceRealtimeCoordinator realtime =
                new AttendanceRealtimeCoordinator(
                        realtimeSource,
                        realtimeWorker,
                        callbackExecutor);
        session = Optional.of(new Session(
                scope,
                repository,
                realtimeSource,
                realtime,
                new AttendanceFeatureRuntime(
                        repository,
                        realtime,
                        worker,
                        organizationId,
                        assignedOnly,
                        canReply,
                        canManage)));
    }

    synchronized void deactivate() {
        Optional<Session> previous = session;
        session = Optional.empty();
        previous.ifPresent(value -> {
            value.realtime().close();
            value.realtimeSource().close();
            value.repository().close();
        });
    }

    synchronized Optional<AttendanceFeatureRuntime> runtime() {
        return session.map(Session::runtime);
    }

    @Override
    public synchronized void close() {
        deactivate();
        worker.shutdownNow();
        realtimeWorker.shutdownNow();
    }

    private static boolean canRead(ShellAccessState access) {
        return access.bootstrap().authorization().has(
                        Capability.ATTENDANCE_READ_ALL)
                || access.bootstrap().authorization().has(
                        Capability.ATTENDANCE_READ_TEAM);
    }

    private record Session(
            AttendanceAccountScope scope,
            RemoteAttendanceRepository repository,
            RemoteAttendanceEventSource realtimeSource,
            AttendanceRealtimeCoordinator realtime,
            AttendanceFeatureRuntime runtime) {
    }
}
