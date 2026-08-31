package br.com.tresvtintas.mobile.core.whatsappadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Connection;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Store;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class WhatsAppAdministrationControllerTest {
    private static final long STORE_ID = 31L;
    private static final long CONNECTION_ID = 77L;

    @Test
    public void loadAndMutationPublishFreshSafeSnapshot() {
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller = controller(repository);

        controller.load();
        assertEquals(
                "Initial mode must come from the repository.",
                WhatsAppChannelMode.DISABLED,
                controller.currentState().snapshot().orElseThrow()
                        .stores().get(0).mode());

        repository.mode = WhatsAppChannelMode.EVOLUTION;
        controller.setMode(STORE_ID, WhatsAppChannelMode.EVOLUTION);

        WhatsAppAdministrationState state = controller.currentState();
        assertEquals(
                "Mutation must end ready.",
                WhatsAppAdministrationState.Phase.READY,
                state.phase());
        assertEquals(
                "Fresh mode must replace the old snapshot.",
                WhatsAppChannelMode.EVOLUTION,
                state.snapshot().orElseThrow().stores().get(0).mode());
        assertEquals(
                "Last action must identify the confirmed command.",
                "set_mode",
                state.lastAction().orElseThrow().action());
        assertFalse(
                "Mutations must not publish QR material.",
                state.ephemeralQr().isPresent());
        assertEquals(
                "Mutation must refresh the snapshot once.",
                2,
                repository.loadCount);
    }

    @Test
    public void qrIsEphemeralAndCanBeClearedWithoutReload() {
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller = controller(repository);
        controller.load();

        controller.requestQr(CONNECTION_ID);
        EphemeralQr qr = controller.currentState().ephemeralQr().orElseThrow();
        assertEquals(
                "Pairing code must be delivered ephemerally.",
                "PAIR-123",
                qr.pairingCode().orElseThrow());
        assertEquals(
                "QR retrieval must not reload stores.",
                1,
                repository.loadCount);

        controller.clearEphemeralQr();
        assertTrue(
                "Explicit clearing must remove QR material.",
                controller.currentState().ephemeralQr().isEmpty());
        assertTrue(
                "Clearing QR must preserve the safe snapshot.",
                controller.currentState().snapshot().isPresent());
    }

    @Test
    public void failureKeepsPreviousSnapshotAndDoesNotExposeQr() {
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller = controller(repository);
        controller.load();
        repository.failure = new WhatsAppAdministrationException(
                WhatsAppAdministrationFailureKind.FORBIDDEN,
                "denied");

        controller.provisionEvolution(STORE_ID);

        WhatsAppAdministrationState state = controller.currentState();
        assertTrue("Previous snapshot must survive failure.", state.snapshot().isPresent());
        assertEquals(
                "Failure kind must be preserved.",
                WhatsAppAdministrationFailureKind.FORBIDDEN,
                state.failure().orElseThrow().kind());
        assertTrue(
                "Failure must not publish QR material.",
                state.ephemeralQr().isEmpty());
    }

    @Test
    public void invalidMetaIdentifiersFailBeforeNetworkCall() {
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller = controller(repository);
        controller.load();

        assertThrows(
                "Invalid public identifier must fail locally.",
                IllegalArgumentException.class,
                () -> controller.configureMeta(
                        STORE_ID,
                        "not-public-id",
                        Optional.empty()));
        assertEquals(
                "Invalid input must not reach the repository.",
                0,
                repository.mutationCount);
    }

    @Test
    public void closeDropsLateWorkAndClearsSensitiveState() {
        DeferredExecutor worker = new DeferredExecutor();
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller =
                new WhatsAppAdministrationController(
                        repository,
                        worker,
                        Runnable::run);
        controller.load();
        controller.close();
        worker.run();

        assertEquals(
                "Closed controller must reject late completion.",
                WhatsAppAdministrationState.Phase.CLOSED,
                controller.currentState().phase());
        assertTrue(
                "Close must clear safe snapshots.",
                controller.currentState().snapshot().isEmpty());
        assertTrue(
                "Close must clear QR material.",
                controller.currentState().ephemeralQr().isEmpty());
    }

    @Test
    public void pauseDropsLateQrAndKeepsOnlyTheSafeSnapshot() {
        DeferredExecutor worker = new DeferredExecutor();
        FakeRepository repository = new FakeRepository();
        WhatsAppAdministrationController controller =
                new WhatsAppAdministrationController(
                        repository,
                        worker,
                        Runnable::run);
        controller.load();
        worker.run();
        controller.requestQr(CONNECTION_ID);

        controller.pause();
        worker.run();

        assertEquals(
                "Paused administration must return to a safe ready state.",
                WhatsAppAdministrationState.Phase.READY,
                controller.currentState().phase());
        assertTrue(
                "Pause must preserve the non-sensitive store projection.",
                controller.currentState().snapshot().isPresent());
        assertTrue(
                "A QR arriving after pause must be discarded.",
                controller.currentState().ephemeralQr().isEmpty());
    }

    private static WhatsAppAdministrationController controller(
            FakeRepository repository) {
        return new WhatsAppAdministrationController(
                repository,
                Runnable::run,
                Runnable::run);
    }

    private static Snapshot snapshot(WhatsAppChannelMode mode) {
        Connection connection = new Connection(
                CONNECTION_ID,
                STORE_ID,
                WhatsAppProvider.EVOLUTION,
                "WhatsApp Evolution",
                WhatsAppConnectionStatus.DISCONNECTED,
                true,
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of("store-31"),
                false,
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-08-01T12:00:00Z"));
        return new Snapshot(List.of(new Store(
                STORE_ID,
                "jundiai",
                "3V Tintas Jundiaí",
                WhatsAppStoreStatus.ACTIVE,
                mode,
                List.of(connection))));
    }

    private static final class FakeRepository
            implements WhatsAppAdministrationRepository {
        private WhatsAppChannelMode mode = WhatsAppChannelMode.DISABLED;
        private WhatsAppAdministrationException failure;
        private int loadCount;
        private int mutationCount;

        @Override
        public Snapshot load() throws WhatsAppAdministrationException {
            loadCount++;
            failIfConfigured();
            return snapshot(mode);
        }

        @Override
        public ActionResult configureMeta(
                long organizationId,
                String phoneNumberId,
                Optional<String> phoneNumber,
                String idempotencyKey) throws WhatsAppAdministrationException {
            return action("configure_meta", organizationId);
        }

        @Override
        public ActionResult provisionEvolution(
                long organizationId,
                String idempotencyKey) throws WhatsAppAdministrationException {
            return action("provision_evolution", organizationId);
        }

        @Override
        public ActionResult setMode(
                long organizationId,
                WhatsAppChannelMode requestedMode,
                String idempotencyKey) throws WhatsAppAdministrationException {
            mode = requestedMode;
            return action("set_mode", organizationId);
        }

        @Override
        public ActionResult refreshEvolution(
                long connectionId,
                String idempotencyKey) throws WhatsAppAdministrationException {
            return action("refresh_evolution", STORE_ID);
        }

        @Override
        public EphemeralQr requestQr(long connectionId)
                throws WhatsAppAdministrationException {
            failIfConfigured();
            return new EphemeralQr(
                    STORE_ID,
                    connectionId,
                    Optional.empty(),
                    Optional.of("PAIR-123"));
        }

        private ActionResult action(String action, long organizationId)
                throws WhatsAppAdministrationException {
            mutationCount++;
            failIfConfigured();
            return new ActionResult(
                    action,
                    organizationId,
                    Optional.of(CONNECTION_ID),
                    Optional.of(mode),
                    Optional.of(WhatsAppConnectionStatus.DISCONNECTED),
                    Optional.empty(),
                    false);
        }

        private void failIfConfigured() throws WhatsAppAdministrationException {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static final class DeferredExecutor implements java.util.concurrent.Executor {
        private Runnable pending;

        @Override
        public void execute(Runnable command) {
            pending = command;
        }

        void run() {
            pending.run();
        }
    }
}
