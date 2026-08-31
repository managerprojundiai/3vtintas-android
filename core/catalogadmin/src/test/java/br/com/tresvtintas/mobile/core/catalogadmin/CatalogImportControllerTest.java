package br.com.tresvtintas.mobile.core.catalogadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Action;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Preview;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Row;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.RowStatus;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Status;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class CatalogImportControllerTest {
    private static final String IMPORT_ID =
            "10000000-0000-4000-8000-000000000011";
    private static final String FILE_NAME = "catalog.csv";

    @Test
    public void uploadFetchesServerAuthoredPreviewAndKeepsSelectedFile() {
        FakeRepository repository = new FakeRepository();
        List<CatalogImportState> states = new ArrayList<>();
        CatalogImportController controller = controller(
                repository,
                Runnable::run);
        SourceFile source = source(FILE_NAME);
        controller.subscribe(states::add);

        controller.select(source);
        controller.upload();

        CatalogImportState state = states.get(states.size() - 1);
        assertEquals(
                "The validated preview must become ready.",
                CatalogImportState.Phase.READY,
                state.phase());
        assertEquals(
                "Only server-authored rows may reach the preview.",
                1,
                state.rows().size());
        assertEquals(
                "The selected local file remains identified for review.",
                FILE_NAME,
                state.source().orElseThrow().name());
        assertTrue(
                "The server digest and revision make the preview confirmable.",
                state.batch().orElseThrow().confirmable());
    }

    @Test
    public void paginationAppendsRowsUsingOnlyTheOpaqueCursor() {
        FakeRepository repository = new FakeRepository();
        List<CatalogImportState> states = new ArrayList<>();
        CatalogImportController controller = controller(
                repository,
                Runnable::run);
        controller.subscribe(states::add);
        controller.select(source(FILE_NAME));
        controller.upload();

        controller.loadMore();

        CatalogImportState state = states.get(states.size() - 1);
        assertEquals(
                "Both authorized pages must remain visible.",
                2,
                state.rows().size());
        assertEquals(
                "The opaque cursor must be sent without reinterpretation.",
                Optional.of("1"),
                repository.lastCursor);
        assertTrue(
                "The final page must remove pagination.",
                state.nextCursor().isEmpty());
    }

    @Test
    public void confirmationUsesBoundRevisionAndDigest() {
        FakeRepository repository = new FakeRepository();
        CatalogImportController controller = controller(
                repository,
                Runnable::run);
        controller.select(source(FILE_NAME));
        controller.upload();

        controller.confirm();

        assertEquals(
                "Confirmation must bind the displayed revision.",
                3,
                repository.confirmedRevision);
        assertEquals(
                "Confirmation must bind the server-authored preview digest.",
                "c".repeat(64),
                repository.confirmedDigest);
        assertFalse(
                "A completed import cannot be confirmed again.",
                repository.lastBatch.confirmable());
    }

    @Test
    public void authorizationLossClearsFileAndPreviouslyVisibleRows() {
        FakeRepository repository = new FakeRepository();
        List<CatalogImportState> states = new ArrayList<>();
        CatalogImportController controller = controller(
                repository,
                Runnable::run);
        controller.subscribe(states::add);
        controller.select(source("private.csv"));
        controller.upload();
        repository.failure = new CatalogAdministrationException(
                CatalogAdministrationFailureKind.FORBIDDEN,
                "Scope revoked.");

        controller.refresh();

        CatalogImportState state = states.get(states.size() - 1);
        assertEquals(
                "Revocation must end in an explicit error state.",
                CatalogImportState.Phase.ERROR,
                state.phase());
        assertTrue(
                "Revoked preview rows cannot remain rendered.",
                state.rows().isEmpty());
        assertTrue(
                "The selected file must be removed from revoked UI state.",
                state.source().isEmpty());
        assertTrue(
                "The import identifier must also be removed.",
                state.batch().isEmpty());
    }

    @Test
    public void newlySelectedFileSuppressesAnOlderQueuedUpload() {
        QueueExecutor worker = new QueueExecutor();
        List<CatalogImportState> states = new ArrayList<>();
        CatalogImportController controller = controller(
                new FakeRepository(),
                worker);
        controller.subscribe(states::add);
        controller.select(source("old.csv"));
        controller.upload();

        controller.select(source("new.csv"));
        worker.runNext();

        CatalogImportState state = states.get(states.size() - 1);
        assertEquals(
                "A stale upload cannot replace the new selection.",
                CatalogImportState.Phase.FILE_READY,
                state.phase());
        assertEquals(
                "The latest selected file must remain authoritative.",
                "new.csv",
                state.source().orElseThrow().name());
    }

    private static CatalogImportController controller(
            CatalogImportRepository repository,
            Executor worker) {
        return new CatalogImportController(
                repository,
                worker,
                Runnable::run);
    }

    private static SourceFile source(String name) {
        return new SourceFile(
                name,
                "text/csv",
                "a".repeat(64),
                "sku,name\nSKU-1,Tinta".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Batch batch(
            Status status,
            int revision,
            List<Row> rows,
            Optional<String> cursor) {
        return new Batch(
                IMPORT_ID,
                FILE_NAME,
                status,
                revision,
                status == Status.PREVIEW_READY
                        ? Optional.of("c".repeat(64))
                        : Optional.empty(),
                2,
                status == Status.PREVIEW_READY ? 2 : 0,
                0,
                status == Status.COMPLETED ? 2 : 0,
                0,
                Optional.empty(),
                Instant.parse("2026-08-01T12:00:00Z"),
                rows,
                cursor);
    }

    private static Row row(int number) {
        return new Row(
                number,
                Action.CREATE,
                RowStatus.READY,
                new Preview(
                        "Product " + number,
                        Optional.of("SKU-" + number),
                        Optional.of("12.90"),
                        Optional.of(5),
                        Optional.of("Tintas")),
                Optional.empty());
    }

    private static final class QueueExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runNext() {
            tasks.remove().run();
        }
    }

    private static final class FakeRepository
            implements CatalogImportRepository {
        private CatalogAdministrationException failure;
        private Optional<String> lastCursor = Optional.empty();
        private int confirmedRevision;
        private String confirmedDigest;
        private Batch lastBatch = batch(
                Status.PREVIEW_READY,
                3,
                List.of(row(1)),
                Optional.of("1"));

        @Override
        public Mutation upload(SourceFile source, String idempotencyKey) {
            return new Mutation(IMPORT_ID, 1, Status.UPLOADED, false);
        }

        @Override
        public Batch get(
                String importId,
                Optional<String> cursor,
                int limit) throws CatalogAdministrationException {
            if (failure != null) {
                throw failure;
            }
            lastCursor = cursor;
            if (cursor.isPresent()) {
                lastBatch = batch(
                        Status.PREVIEW_READY,
                        3,
                        List.of(row(2)),
                        Optional.empty());
            }
            return lastBatch;
        }

        @Override
        public Mutation confirm(
                String importId,
                int expectedRevision,
                String previewDigest,
                String idempotencyKey) {
            confirmedRevision = expectedRevision;
            confirmedDigest = previewDigest;
            lastBatch = batch(
                    Status.COMPLETED,
                    expectedRevision + 2,
                    List.of(row(1), row(2)),
                    Optional.empty());
            return new Mutation(
                    IMPORT_ID,
                    expectedRevision + 1,
                    Status.QUEUED,
                    false);
        }
    }
}
