package br.com.tresvtintas.mobile.core.notifications;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class NotificationSettingsController {
    private final NotificationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<NotificationSettingsStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile NotificationSettingsState current =
            NotificationSettingsState.empty();

    public NotificationSettingsController(
            NotificationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Notification repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(NotificationSettingsStateListener listener) {
        NotificationSettingsStateListener required = Objects.requireNonNull(
                listener,
                "Notification listener is required.");
        listeners.add(required);
        NotificationSettingsState snapshot = current;
        main.execute(() -> required.onNotificationSettingsStateChanged(snapshot));
    }

    public void unsubscribe(NotificationSettingsStateListener listener) {
        listeners.remove(listener);
    }

    public NotificationSettingsState currentState() {
        return current;
    }

    public void load() {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(NotificationSettingsState.loading());
        worker.execute(() -> {
            try {
                complete(operation, NotificationSettingsState.ready(repository.load()));
            } catch (NotificationException failure) {
                complete(operation, NotificationSettingsState.error(failure));
            }
        });
    }

    public void update(
            NotificationPermissionState permissionState,
            boolean operationalEnabled,
            Map<NotificationCategory, Boolean> categories) {
        Optional<NotificationPreferences> previous = current.preferences();
        if (previous.isEmpty() || !busy.compareAndSet(false, true)) {
            return;
        }
        NotificationPreferences selected = previous.orElseThrow()
                .withPermission(permissionState)
                .withSelection(operationalEnabled, categories);
        long operation = generation.incrementAndGet();
        publish(NotificationSettingsState.saving(selected));
        worker.execute(() -> save(operation, previous.orElseThrow(), selected));
    }

    public void registerInstallation(String firebaseInstallationId) {
        if (firebaseInstallationId == null || firebaseInstallationId.isBlank()) {
            return;
        }
        long operation = generation.get();
        worker.execute(() -> {
            try {
                repository.register(firebaseInstallationId);
            } catch (NotificationException failure) {
                warn(operation, failure);
            }
        });
    }

    public void unregisterInstallation() {
        long operation = generation.get();
        worker.execute(() -> {
            try {
                repository.unregister();
            } catch (NotificationException failure) {
                warn(operation, failure);
            }
        });
    }

    public void transportUnavailable() {
        warn(
                generation.get(),
                new NotificationException(
                        NotificationFailureKind.CONFIGURATION,
                        "Notification transport is unavailable."));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(NotificationSettingsState.closed());
        listeners.clear();
    }

    private void save(
            long operation,
            NotificationPreferences previous,
            NotificationPreferences selected) {
        try {
            NotificationPreferences saved = repository.update(
                    selected.permissionState(),
                    selected.operationalEnabled(),
                    selected.categories(),
                    previous.revision());
            complete(operation, NotificationSettingsState.ready(saved));
        } catch (NotificationException failure) {
            if (failure.kind() == NotificationFailureKind.CONFLICT) {
                try {
                    complete(
                            operation,
                            NotificationSettingsState.ready(
                                    repository.load(),
                                    failure));
                    return;
                } catch (NotificationException reloadFailure) {
                    complete(
                            operation,
                            NotificationSettingsState.error(reloadFailure));
                    return;
                }
            }
            complete(
                    operation,
                    NotificationSettingsState.ready(previous, failure));
        }
    }

    private void warn(long operation, NotificationException failure) {
        NotificationSettingsState snapshot = current;
        if (generation.get() == operation && snapshot.preferences().isPresent()) {
            publish(NotificationSettingsState.ready(
                    snapshot.preferences().orElseThrow(),
                    failure));
        }
    }

    private void complete(long operation, NotificationSettingsState state) {
        if (generation.get() != operation
                || current.phase() == NotificationSettingsState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(NotificationSettingsState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onNotificationSettingsStateChanged(state)));
    }
}
