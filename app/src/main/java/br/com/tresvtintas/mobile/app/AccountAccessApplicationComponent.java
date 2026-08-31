package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.auth.AuthenticatedSession;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.data.accountaccess.AccountAccessScope;
import br.com.tresvtintas.mobile.data.accountaccess.RemoteAccountAccessRepository;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessFeatureRuntime;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns self-service account access data. Its scope is bound to the authenticated user, session,
 * device, and authorization revision; it never accepts a role, store, or another user identifier.
 */
final class AccountAccessApplicationComponent
        implements AutoCloseable {
    private final MobileApi protectedApi;
    private final ExecutorService worker;
    private final Runnable sessionRejectionHandler;
    private Optional<Session> session = Optional.empty();

    AccountAccessApplicationComponent(
            MobileApi protectedApi,
            Runnable sessionRejectionHandler) {
        this.protectedApi = Objects.requireNonNull(
                protectedApi,
                "Protected account access API is required.");
        this.sessionRejectionHandler = Objects.requireNonNull(
                sessionRejectionHandler,
                "Account access rejection handler is required.");
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "3v-account-access-worker");
            thread.setDaemon(false);
            return thread;
        });
    }

    synchronized void activate(
            ShellAccessState access,
            AuthenticatedSession authenticated) {
        if (!valid(access, authenticated) || worker.isShutdown()) {
            deactivate();
            return;
        }
        AccountAccessScope scope = new AccountAccessScope(
                authenticated.user().id(),
                authenticated.session().id(),
                authenticated.session().deviceId(),
                access.bootstrap().authorization().revision());
        if (session.map(Session::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivate();
        RemoteAccountAccessRepository repository =
                new RemoteAccountAccessRepository(
                        scope,
                        protectedApi);
        session = Optional.of(new Session(
                scope,
                repository,
                new AccountAccessFeatureRuntime(
                        repository,
                        worker,
                        sessionRejectionHandler)));
    }

    synchronized void deactivate() {
        Optional<Session> previous = session;
        session = Optional.empty();
        previous.ifPresent(value -> value.repository().close());
    }

    synchronized Optional<AccountAccessFeatureRuntime> runtime() {
        return session.map(Session::runtime);
    }

    @Override
    public synchronized void close() {
        deactivate();
        worker.shutdownNow();
    }

    private static boolean valid(
            ShellAccessState access,
            AuthenticatedSession authenticated) {
        return access != null
                && authenticated != null
                && access.bootstrap().user().id()
                        == authenticated.user().id()
                && access.bootstrap().session().id().equals(
                        authenticated.session().id())
                && access.bootstrap().session().deviceId().equals(
                        authenticated.session().deviceId());
    }

    private record Session(
            AccountAccessScope scope,
            RemoteAccountAccessRepository repository,
            AccountAccessFeatureRuntime runtime) {
    }
}
