package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Row;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import java.util.List;
import java.util.Optional;

public record CatalogImportState(
        Phase phase,
        Optional<SourceFile> source,
        Optional<Batch> batch,
        List<Row> rows,
        Optional<String> nextCursor,
        Optional<CatalogAdministrationException> failure,
        boolean loadingMore) {
    public enum Phase {
        EMPTY,
        FILE_READY,
        WORKING,
        READY,
        ERROR,
        CLOSED
    }

    public CatalogImportState {
        phase = phase == null ? Phase.EMPTY : phase;
        source = source == null ? Optional.empty() : source;
        batch = batch == null ? Optional.empty() : batch;
        rows = rows == null ? List.of() : List.copyOf(rows);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        failure = failure == null ? Optional.empty() : failure;
    }

    @Override
    public List<Row> rows() {
        return List.copyOf(rows);
    }

    public static CatalogImportState empty() {
        return new CatalogImportState(
                Phase.EMPTY,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                false);
    }
}
