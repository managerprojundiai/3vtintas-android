package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import java.util.Optional;

public interface CatalogImportRepository {
    Mutation upload(SourceFile source, String idempotencyKey)
            throws CatalogAdministrationException;

    Batch get(String importId, Optional<String> cursor, int limit)
            throws CatalogAdministrationException;

    Mutation confirm(
            String importId,
            int expectedRevision,
            String previewDigest,
            String idempotencyKey) throws CatalogAdministrationException;
}
