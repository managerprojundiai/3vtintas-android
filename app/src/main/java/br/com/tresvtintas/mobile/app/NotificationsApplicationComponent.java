package br.com.tresvtintas.mobile.app;

import android.content.Context;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.notifications.FirebaseClientConfiguration;
import br.com.tresvtintas.mobile.core.notifications.NotificationDisplayPolicy;
import br.com.tresvtintas.mobile.core.notifications.NotificationDisplayPolicyStore;
import br.com.tresvtintas.mobile.core.notifications.NotificationException;
import br.com.tresvtintas.mobile.core.notifications.NotificationMessage;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionController;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationPreferences;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushRegistry;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushSink;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushTransport;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsController;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsState;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsStateListener;
import br.com.tresvtintas.mobile.data.notifications.NotificationAccountScope;
import br.com.tresvtintas.mobile.data.notifications.RemoteNotificationRepository;
import br.com.tresvtintas.mobile.feature.notifications.NotificationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.platform.notifications.AndroidNotificationDisplayPolicyStore;
import br.com.tresvtintas.mobile.platform.notifications.AndroidNotificationPermissionController;
import br.com.tresvtintas.mobile.platform.notifications.FirebasePushTransport;
import br.com.tresvtintas.mobile.platform.notifications.NotificationChannels;
import br.com.tresvtintas.mobile.platform.notifications.NotificationPublisher;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class NotificationsApplicationComponent
        implements NotificationPushSink, AutoCloseable {
    private final MobileApi api;
    private final Executor main;
    private final ExecutorService worker;
    private final NotificationPermissionController permissionController;
    private final NotificationDisplayPolicyStore displayPolicyStore;
    private final NotificationPushTransport transport;
    private final NotificationPublisher publisher;
    private final NotificationSettingsStateListener stateListener =
            this::reconcile;
    private Optional<Session> session = Optional.empty();
    private RegistrationMode registrationMode = RegistrationMode.UNKNOWN;

    NotificationsApplicationComponent(
            Context context,
            MobileApi api,
            Executor main,
            FirebaseClientConfiguration firebaseConfiguration) {
        this.api = api;
        this.main = main;
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-notifications-worker");
            thread.setDaemon(false);
            return thread;
        });
        permissionController =
                new AndroidNotificationPermissionController(context);
        displayPolicyStore = new AndroidNotificationDisplayPolicyStore(context);
        transport = new FirebasePushTransport(context, firebaseConfiguration);
        publisher = new NotificationPublisher(context);
        NotificationChannels.ensureCreated(context);
        NotificationPushRegistry.install(this);
    }

    synchronized void activate(ShellAccessState access) {
        if (access == null || !access.isOperational()) {
            deactivate(true);
            return;
        }
        NotificationAccountScope scope = new NotificationAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (session.map(Session::scope)
                .filter(current -> sameScope(current, scope))
                .isPresent()) {
            return;
        }
        closeSession(false);
        displayPolicyStore.clear();
        RemoteNotificationRepository repository =
                new RemoteNotificationRepository(scope, api);
        NotificationSettingsController controller =
                new NotificationSettingsController(repository, worker, main);
        Session created = new Session(
                scope,
                repository,
                controller,
                new NotificationFeatureRuntime(
                        controller,
                        permissionController,
                        transport.available()));
        session = Optional.of(created);
        registrationMode = RegistrationMode.UNKNOWN;
        controller.subscribe(stateListener);
        controller.load();
    }

    synchronized void deactivate(boolean unregister) {
        if (unregister) {
            displayPolicyStore.clear();
        }
        closeSession(unregister);
    }

    synchronized Optional<NotificationFeatureRuntime> runtime() {
        return session.map(Session::runtime);
    }

    @Override
    public synchronized void onRegistered(String firebaseInstallationId) {
        session.filter(this::canReceivePush)
                .ifPresent(current ->
                        current.controller().registerInstallation(
                                firebaseInstallationId));
        if (session.isEmpty() || !canReceivePush(session.orElseThrow())) {
            transport.unregister();
        }
    }

    @Override
    public void onUnregistered() {
        // The server-side registration is erased by the authenticated DELETE call.
    }

    @Override
    public synchronized void onMessage(NotificationMessage message) {
        boolean allowed = session.map(current -> canDisplay(current, message))
                .orElseGet(() -> displayPolicyStore.load()
                        .map(policy -> policy.allows(message))
                        .orElse(false));
        if (allowed) {
            publisher.publish(message);
        }
    }

    @Override
    public synchronized void onMessagesDeleted() {
        session.ifPresent(current -> current.controller().load());
    }

    @Override
    public synchronized void onTransportFailure() {
        session.ifPresent(current ->
                current.controller().transportUnavailable());
    }

    @Override
    public synchronized void close() {
        displayPolicyStore.clear();
        closeSession(true);
        NotificationPushRegistry.remove(this);
        worker.shutdownNow();
    }

    private synchronized void reconcile(NotificationSettingsState state) {
        if (state.phase() != NotificationSettingsState.Phase.READY
                || state.preferences().isEmpty()
                || session.isEmpty()) {
            return;
        }
        NotificationPreferences preferences =
                state.preferences().orElseThrow();
        session.orElseThrow().preferences = preferences;
        NotificationPermissionState platform =
                permissionController.currentState();
        if (platform != NotificationPermissionState.UNKNOWN
                && platform != preferences.permissionState()) {
            displayPolicyStore.clear();
            session.orElseThrow().controller().update(
                    platform,
                    preferences.operationalEnabled(),
                    preferences.categories());
            return;
        }
        boolean enable = canReceivePush(session.orElseThrow());
        if (enable) {
            displayPolicyStore.save(NotificationDisplayPolicy.from(preferences));
        } else {
            displayPolicyStore.clear();
        }
        RegistrationMode next = enable
                ? RegistrationMode.ENABLED
                : RegistrationMode.DISABLED;
        if (registrationMode == next) {
            return;
        }
        registrationMode = next;
        if (enable) {
            transport.register();
        } else {
            session.orElseThrow().controller().unregisterInstallation();
            transport.unregister();
        }
    }

    private boolean canReceivePush(Session current) {
        NotificationPreferences preferences = current.preferences;
        return transport.available()
                && preferences != null
                && permissionController.currentState()
                == NotificationPermissionState.GRANTED
                && preferences.permissionState()
                == NotificationPermissionState.GRANTED;
    }

    private boolean canDisplay(
            Session current,
            NotificationMessage message) {
        if (!canReceivePush(current)) {
            return false;
        }
        NotificationPreferences preferences = current.preferences;
        return message.category().configurable()
                ? preferences.operationalEnabled()
                        && preferences.enabled(message.category())
                : preferences.essentialSecurityAlerts();
    }

    private void closeSession(boolean unregister) {
        Optional<Session> previous = session;
        session = Optional.empty();
        registrationMode = RegistrationMode.UNKNOWN;
        previous.ifPresent(current -> {
            current.controller().unsubscribe(stateListener);
            current.controller().close();
            if (unregister) {
                worker.execute(() -> unregisterAndClose(current));
                transport.unregister();
            } else {
                current.repository().close();
            }
        });
    }

    private static void unregisterAndClose(Session current) {
        try {
            current.repository().unregister();
        } catch (NotificationException ignored) {
            // Server expiry and future registration reconciliation remain fail-safe.
        } finally {
            current.repository().close();
        }
    }

    private static boolean sameScope(
            NotificationAccountScope first,
            NotificationAccountScope second) {
        return first.userId() == second.userId()
                && first.authorizationRevision().equals(
                        second.authorizationRevision());
    }

    private enum RegistrationMode {
        UNKNOWN,
        ENABLED,
        DISABLED
    }

    private static final class Session {
        private final NotificationAccountScope scope;
        private final RemoteNotificationRepository repository;
        private final NotificationSettingsController controller;
        private final NotificationFeatureRuntime runtime;
        private volatile NotificationPreferences preferences;

        private Session(
                NotificationAccountScope scope,
                RemoteNotificationRepository repository,
                NotificationSettingsController controller,
                NotificationFeatureRuntime runtime) {
            this.scope = scope;
            this.repository = repository;
            this.controller = controller;
            this.runtime = runtime;
        }

        NotificationAccountScope scope() {
            return scope;
        }

        RemoteNotificationRepository repository() {
            return repository;
        }

        NotificationSettingsController controller() {
            return controller;
        }

        NotificationFeatureRuntime runtime() {
            return runtime;
        }
    }
}
