package br.com.tresvtintas.mobile.core.systemconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class SystemConfigurationControllerTest {
    private static final Values VALUES = new Values(
            "3V Tintas Jundiaí",
            "11 4000-0000",
            "Rua das Tintas, 3",
            "Bellarte Pinturas",
            "11 4999-0000",
            "3.00",
            false);
    private static final Snapshot SNAPSHOT = new Snapshot(
            VALUES,
            7,
            Instant.parse("2026-07-31T12:00:00Z"));

    @Test
    public void loadPublishesAuthorizedSnapshot() {
        FakeRepository repository = new FakeRepository();
        SystemConfigurationController controller = controller(repository);

        controller.load();

        assertEquals("Controller must become ready.",
                SystemConfigurationState.Phase.READY,
                controller.currentState().phase());
        assertEquals("Authorized snapshot must be published.", SNAPSHOT,
                controller.currentState().configuration().orElseThrow());
        assertEquals("Repository must load once.", 1, repository.loadCount);
    }

    @Test
    public void updateUsesCurrentRevisionAndUniqueKey() {
        FakeRepository repository = new FakeRepository();
        SystemConfigurationController controller = controller(repository);
        controller.load();
        Values changed = new Values(
                VALUES.storeName(),
                VALUES.storePhone(),
                VALUES.storeAddress(),
                VALUES.laborCompanyName(),
                VALUES.laborCompanyContact(),
                "4.00",
                false);

        controller.update(changed);

        assertEquals("Revision must be sent.", 7, repository.expectedRevision);
        assertFalse("Idempotency key is required.",
                repository.idempotencyKey.isBlank());
        assertTrue("Changed result must be exposed.",
                controller.currentState().changed());
        assertEquals("Revision must advance.", 8,
                controller.currentState().configuration().orElseThrow().revision());
    }

    @Test
    public void conflictReloadsInsteadOfOverwritingRemoteState() {
        FakeRepository repository = new FakeRepository();
        SystemConfigurationController controller = controller(repository);
        controller.load();
        repository.updateFailure = failure(
                SystemConfigurationFailureKind.CONFLICT);
        repository.snapshot = new Snapshot(VALUES, 9, Instant.now());

        controller.update(VALUES);

        assertEquals("Conflict must trigger reload.", 2, repository.loadCount);
        assertEquals("Remote revision must win.", 9,
                controller.currentState().configuration().orElseThrow().revision());
        assertEquals("Conflict must remain visible.",
                SystemConfigurationFailureKind.CONFLICT,
                controller.currentState().failure().orElseThrow().kind());
    }

    @Test
    public void networkFailureKeepsLastConfirmedSnapshot() {
        FakeRepository repository = new FakeRepository();
        SystemConfigurationController controller = controller(repository);
        controller.load();
        repository.updateFailure = failure(
                SystemConfigurationFailureKind.NETWORK);

        controller.update(VALUES);

        assertEquals("Confirmed snapshot must remain visible.", SNAPSHOT,
                controller.currentState().configuration().orElseThrow());
        assertEquals("Network failure must remain visible.",
                SystemConfigurationFailureKind.NETWORK,
                controller.currentState().failure().orElseThrow().kind());
    }

    @Test
    public void closeInvalidatesFurtherCallbacks() {
        FakeRepository repository = new FakeRepository();
        SystemConfigurationController controller = controller(repository);
        List<SystemConfigurationState.Phase> phases = new ArrayList<>();
        controller.subscribe(state -> phases.add(state.phase()));

        controller.close();
        controller.load();

        assertEquals("Controller must stay closed.",
                SystemConfigurationState.Phase.CLOSED,
                controller.currentState().phase());
        assertTrue("Closed phase must be published.",
                phases.contains(SystemConfigurationState.Phase.CLOSED));
    }

    private static SystemConfigurationController controller(
            SystemConfigurationRepository repository) {
        return new SystemConfigurationController(
                repository,
                Runnable::run,
                Runnable::run);
    }

    private static SystemConfigurationException failure(
            SystemConfigurationFailureKind kind) {
        return new SystemConfigurationException(kind, "test failure");
    }

    private static final class FakeRepository
            implements SystemConfigurationRepository {
        private Snapshot snapshot = SNAPSHOT;
        private SystemConfigurationException updateFailure;
        private int loadCount;
        private int expectedRevision;
        private String idempotencyKey = "";

        @Override
        public Snapshot load() {
            loadCount++;
            return snapshot;
        }

        @Override
        public Mutation update(
                Values values,
                int revision,
                String key) throws SystemConfigurationException {
            expectedRevision = revision;
            idempotencyKey = key;
            if (updateFailure != null) {
                throw updateFailure;
            }
            snapshot = new Snapshot(values, revision + 1, Instant.now());
            return new Mutation(snapshot, true, false);
        }
    }
}
