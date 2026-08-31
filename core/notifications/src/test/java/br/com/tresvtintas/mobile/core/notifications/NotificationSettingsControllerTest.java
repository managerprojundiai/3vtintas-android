package br.com.tresvtintas.mobile.core.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public final class NotificationSettingsControllerTest {
    @Test
    public void loadsAndPublishesAuthorizedPreferences() {
        QueueExecutor worker = new QueueExecutor();
        List<NotificationSettingsState> states = new ArrayList<>();
        NotificationSettingsController controller =
                new NotificationSettingsController(
                        new FakeRepository(preferences(3)),
                        worker,
                        Runnable::run);
        controller.subscribe(states::add);

        controller.load();
        worker.runNext();

        assertEquals(
                "Loading must publish empty, loading and ready phases.",
                List.of(
                        NotificationSettingsState.Phase.EMPTY,
                        NotificationSettingsState.Phase.LOADING,
                        NotificationSettingsState.Phase.READY),
                states.stream().map(NotificationSettingsState::phase).toList());
        assertEquals(
                "The server revision must reach the ready state.",
                3,
                states.get(2).preferences().orElseThrow().revision());
    }

    @Test
    public void updateUsesTheDisplayedRevisionAndOneCompleteDocument() {
        QueueExecutor worker = new QueueExecutor();
        FakeRepository repository = new FakeRepository(preferences(4));
        NotificationSettingsController controller =
                new NotificationSettingsController(
                        repository,
                        worker,
                        Runnable::run);
        controller.load();
        worker.runNext();

        controller.update(
                NotificationPermissionState.DENIED,
                false,
                categories(false));
        worker.runNext();

        assertEquals(
                "The displayed revision must protect the update.",
                4,
                repository.expectedRevision);
        assertEquals(
                "The platform permission decision must be sent.",
                NotificationPermissionState.DENIED,
                repository.permissionState);
        assertEquals(
                "The repository must return the authoritative new revision.",
                5,
                controller.currentState()
                        .preferences().orElseThrow().revision());
    }

    @Test
    public void conflictReloadsInsteadOfOverwritingAnotherDeviceDecision() {
        QueueExecutor worker = new QueueExecutor();
        FakeRepository repository = new FakeRepository(preferences(8));
        repository.conflictOnUpdate = true;
        NotificationSettingsController controller =
                new NotificationSettingsController(
                        repository,
                        worker,
                        Runnable::run);
        controller.load();
        worker.runNext();

        controller.update(
                NotificationPermissionState.GRANTED,
                true,
                categories(false));
        worker.runNext();

        NotificationSettingsState state = controller.currentState();
        assertEquals(
                "A conflict must recover to the authoritative ready state.",
                NotificationSettingsState.Phase.READY,
                state.phase());
        assertEquals(
                "A conflict must force a second server read.",
                2,
                repository.loadCalls.get());
        assertEquals(
                "The warning must stay typed without exposing server text.",
                Optional.of(NotificationFailureKind.CONFLICT),
                state.failure());
    }

    @Test
    public void closeInvalidatesQueuedResultsAndFutureStatePublication() {
        QueueExecutor worker = new QueueExecutor();
        List<NotificationSettingsState> states = new ArrayList<>();
        NotificationSettingsController controller =
                new NotificationSettingsController(
                        new FakeRepository(preferences(1)),
                        worker,
                        Runnable::run);
        controller.subscribe(states::add);

        controller.load();
        controller.close();
        worker.runNext();

        assertEquals(
                "A queued result must never revive a closed account scope.",
                NotificationSettingsState.Phase.CLOSED,
                controller.currentState().phase());
        assertEquals(
                "Closing must be the final observable phase.",
                NotificationSettingsState.Phase.CLOSED,
                states.get(states.size() - 1).phase());
    }

    @Test
    public void registrationIgnoresBlankInstallationIdentifiers() {
        QueueExecutor worker = new QueueExecutor();
        FakeRepository repository = new FakeRepository(preferences(1));
        NotificationSettingsController controller =
                new NotificationSettingsController(
                        repository,
                        worker,
                        Runnable::run);

        controller.registerInstallation(" ");

        assertTrue(
                "A blank identifier must not schedule network work.",
                worker.isEmpty());
        assertEquals(
                "A blank identifier must not reach persistence.",
                0,
                repository.registerCalls.get());
    }

    private static NotificationPreferences preferences(int revision) {
        return new NotificationPreferences(
                NotificationPermissionState.GRANTED,
                true,
                categories(true),
                true,
                revision,
                Optional.of(Instant.parse("2026-07-28T12:00:00Z")));
    }

    private static Map<NotificationCategory, Boolean> categories(
            boolean enabled) {
        Map<NotificationCategory, Boolean> selected =
                new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable()) {
                selected.put(category, enabled);
            }
        }
        return selected;
    }

    private static final class QueueExecutor implements Executor {
        private final Queue<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
        }

        void runNext() {
            pending.remove().run();
        }

        boolean isEmpty() {
            return pending.isEmpty();
        }
    }

    private static final class FakeRepository
            implements NotificationRepository {
        private final NotificationPreferences initial;
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger registerCalls = new AtomicInteger();
        private boolean conflictOnUpdate;
        private int expectedRevision = -1;
        private NotificationPermissionState permissionState;

        FakeRepository(NotificationPreferences initial) {
            this.initial = initial;
        }

        @Override
        public NotificationPreferences load() {
            loadCalls.incrementAndGet();
            return initial;
        }

        @Override
        public NotificationPreferences update(
                NotificationPermissionState permission,
                boolean operationalEnabled,
                Map<NotificationCategory, Boolean> selected,
                int revision) throws NotificationException {
            expectedRevision = revision;
            permissionState = permission;
            if (conflictOnUpdate) {
                throw new NotificationException(
                        NotificationFailureKind.CONFLICT,
                        "The preference document changed.");
            }
            return new NotificationPreferences(
                    permission,
                    operationalEnabled,
                    selected,
                    true,
                    revision + 1,
                    Optional.of(Instant.parse("2026-07-28T12:01:00Z")));
        }

        @Override
        public void register(String firebaseInstallationId) {
            registerCalls.incrementAndGet();
        }

        @Override
        public void unregister() {
            // No operation is required by this test double.
        }
    }
}
