package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CatalogImportController {
    private static final int PAGE_SIZE = 100;

    @FunctionalInterface
    public interface Listener {
        void onCatalogImportStateChanged(CatalogImportState state);
    }

    private final CatalogImportRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CatalogImportState current = CatalogImportState.empty();

    public CatalogImportController(
            CatalogImportRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        main.execute(() -> required.onCatalogImportStateChanged(current));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void select(SourceFile source) {
        generation.incrementAndGet();
        busy.set(false);
        publish(new CatalogImportState(
                CatalogImportState.Phase.FILE_READY,
                Optional.of(Objects.requireNonNull(source, "Source is required.")),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                false));
    }

    public void upload() {
        Optional<SourceFile> source = current.source();
        if (source.isEmpty() || !begin(false)) {
            return;
        }
        long operation = generation.incrementAndGet();
        worker.execute(() -> {
            try {
                var mutation = repository.upload(
                        source.orElseThrow(),
                        UUID.randomUUID().toString());
                Batch batch = repository.get(
                        mutation.importId(),
                        Optional.empty(),
                        PAGE_SIZE);
                complete(operation, stateFrom(batch, source, false));
            } catch (CatalogAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void refresh() {
        Optional<Batch> batch = current.batch();
        Optional<SourceFile> source = current.source();
        if (batch.isEmpty() || !begin(false)) {
            return;
        }
        long operation = generation.incrementAndGet();
        worker.execute(() -> {
            try {
                Batch updated = repository.get(
                        batch.orElseThrow().id(),
                        Optional.empty(),
                        PAGE_SIZE);
                complete(operation, stateFrom(updated, source, false));
            } catch (CatalogAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void loadMore() {
        Optional<Batch> batch = current.batch();
        Optional<String> cursor = current.nextCursor();
        Optional<SourceFile> source = current.source();
        List<CatalogImportModels.Row> existing = current.rows();
        if (batch.isEmpty() || cursor.isEmpty() || !begin(true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        worker.execute(() -> {
            try {
                Batch page = repository.get(
                        batch.orElseThrow().id(),
                        cursor,
                        PAGE_SIZE);
                List<CatalogImportModels.Row> combined =
                        new ArrayList<>(existing);
                combined.addAll(page.rows());
                complete(operation, new CatalogImportState(
                        CatalogImportState.Phase.READY,
                        source,
                        Optional.of(page),
                        combined,
                        page.nextCursor(),
                        Optional.empty(),
                        false));
            } catch (CatalogAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void confirm() {
        Optional<Batch> batch = current.batch();
        Optional<SourceFile> source = current.source();
        if (batch.isEmpty()
                || !batch.orElseThrow().confirmable()
                || !begin(false)) {
            return;
        }
        Batch preview = batch.orElseThrow();
        long operation = generation.incrementAndGet();
        worker.execute(() -> {
            try {
                repository.confirm(
                        preview.id(),
                        preview.revision(),
                        preview.previewDigest().orElseThrow(),
                        UUID.randomUUID().toString());
                Batch updated = repository.get(
                        preview.id(),
                        Optional.empty(),
                        PAGE_SIZE);
                complete(operation, stateFrom(updated, source, false));
            } catch (CatalogAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(new CatalogImportState(
                CatalogImportState.Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                false));
        listeners.clear();
    }

    private boolean begin(boolean loadingMore) {
        if (!busy.compareAndSet(false, true)) {
            return false;
        }
        publish(new CatalogImportState(
                CatalogImportState.Phase.WORKING,
                current.source(),
                current.batch(),
                current.rows(),
                current.nextCursor(),
                Optional.empty(),
                loadingMore));
        return true;
    }

    private void fail(
            long operation,
            CatalogAdministrationException failure) {
        boolean accessLost = failure.kind()
                == CatalogAdministrationFailureKind.AUTH_REJECTED
                || failure.kind()
                == CatalogAdministrationFailureKind.FORBIDDEN
                || failure.kind()
                == CatalogAdministrationFailureKind.ACCESS_REVOKED;
        complete(operation, new CatalogImportState(
                CatalogImportState.Phase.ERROR,
                accessLost ? Optional.empty() : current.source(),
                accessLost ? Optional.empty() : current.batch(),
                accessLost ? List.of() : current.rows(),
                accessLost ? Optional.empty() : current.nextCursor(),
                Optional.of(failure),
                false));
    }

    private void complete(long operation, CatalogImportState state) {
        if (operation != generation.get()
                || current.phase() == CatalogImportState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(CatalogImportState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onCatalogImportStateChanged(state)));
    }

    private static CatalogImportState stateFrom(
            Batch batch,
            Optional<SourceFile> source,
            boolean loadingMore) {
        return new CatalogImportState(
                CatalogImportState.Phase.READY,
                source,
                Optional.of(batch),
                batch.rows(),
                batch.nextCursor(),
                Optional.empty(),
                loadingMore);
    }
}
