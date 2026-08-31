package br.com.tresvtintas.mobile.data.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Action;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Preview;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Row;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.RowStatus;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Status;
import br.com.tresvtintas.mobile.core.network.dto.CatalogImportDtos;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

final class CatalogImportDtoMapper {
    private CatalogImportDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Mutation mutation(
            CatalogImportDtos.Mutation dto,
            boolean replayed) {
        return new Mutation(
                dto.importId(),
                dto.revision(),
                status(dto.status()),
                replayed);
    }

    static Batch batch(CatalogImportDtos.View dto) {
        return new Batch(
                dto.id(),
                dto.fileName(),
                status(dto.status()),
                dto.revision(),
                Optional.ofNullable(dto.previewDigest()),
                dto.totalRows(),
                dto.readyRows(),
                dto.invalidRows(),
                dto.importedRows(),
                dto.failedRows(),
                Optional.ofNullable(dto.failureCode()),
                Instant.parse(dto.expiresAt()),
                dto.rows().stream()
                        .map(CatalogImportDtoMapper::row)
                        .toList(),
                Optional.ofNullable(dto.nextCursor()));
    }

    private static Row row(CatalogImportDtos.Row dto) {
        return new Row(
                dto.rowNumber(),
                Action.valueOf(upper(dto.action())),
                RowStatus.valueOf(upper(dto.status())),
                preview(dto.preview()),
                Optional.ofNullable(dto.message()));
    }

    private static Preview preview(CatalogImportDtos.Preview dto) {
        return new Preview(
                dto.name(),
                Optional.ofNullable(dto.sku()),
                Optional.ofNullable(dto.price()),
                Optional.ofNullable(dto.stock()),
                Optional.ofNullable(dto.categoryName()));
    }

    private static Status status(String value) {
        return Status.valueOf(upper(value));
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
