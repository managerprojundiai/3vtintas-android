package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CatalogImportDtos {
    private static final int MINIMUM_POSITIVE = 1;
    private static final Set<String> STATUSES = Set.of(
            "uploaded",
            "preparing",
            "preview_ready",
            "queued",
            "running",
            "completed",
            "completed_with_errors",
            "failed",
            "expired");
    private static final Set<String> ACTIONS =
            Set.of("create", "update", "skip");
    private static final Set<String> ROW_STATUSES =
            Set.of("ready", "invalid", "running", "imported", "failed");

    private CatalogImportDtos() {
    }

    public static boolean supportsStatus(String status) {
        return STATUSES.contains(status);
    }

    public record Mutation(
            String importId,
            int revision,
            String status) {
        public Mutation {
            importId = DtoValidation.requireUuid(importId, "Import ID");
            positive(revision, "Import revision");
            status = member(status, STATUSES, "Import status");
        }
    }

    public record Confirmation(
            int expectedRevision,
            String previewDigest,
            boolean confirmed) {
        public Confirmation {
            positive(expectedRevision, "Expected revision");
            previewDigest = digest(previewDigest, "Preview digest");
            if (!confirmed) {
                throw new IllegalArgumentException(
                        "Catalog import confirmation is required.");
            }
        }
    }

    public record Preview(
            String name,
            String sku,
            String price,
            Integer stock,
            String categoryName) {
        public Preview {
            name = DtoValidation.requireText(name, "Product name", 200);
            sku = DtoValidation.optionalText(sku, "SKU", 50);
            price = DtoValidation.optionalText(price, "Price", 11);
            if (price != null && !price.matches("^\\d{1,8}\\.\\d{2}$")) {
                throw new IllegalArgumentException(
                        "Catalog import price is invalid.");
            }
            if (stock != null && (stock < 0 || stock > 2_000_000_000)) {
                throw new IllegalArgumentException(
                        "Catalog import stock is invalid.");
            }
            categoryName = DtoValidation.optionalText(
                    categoryName,
                    "Category name",
                    100);
        }
    }

    public record Row(
            int rowNumber,
            String action,
            String status,
            Preview preview,
            String message) {
        public Row {
            positive(rowNumber, "Row number");
            action = member(action, ACTIONS, "Row action");
            status = member(status, ROW_STATUSES, "Row status");
            if (preview == null) {
                throw new IllegalArgumentException(
                        "Catalog import preview is required.");
            }
            message = DtoValidation.optionalText(
                    message,
                    "Import row message",
                    2_000);
        }
    }

    public record View(
            String id,
            String fileName,
            String mediaType,
            String fileSha256,
            String status,
            int revision,
            String previewDigest,
            int totalRows,
            int readyRows,
            int invalidRows,
            int importedRows,
            int failedRows,
            String failureCode,
            String expiresAt,
            String startedAt,
            String completedAt,
            String createdAt,
            String updatedAt,
            List<Row> rows,
            String nextCursor) {
        public View {
            id = DtoValidation.requireUuid(id, "Import ID");
            fileName = DtoValidation.requireText(fileName, "File name", 255);
            mediaType = member(
                    mediaType,
                    Set.of("csv", "xlsx"),
                    "Import media type");
            fileSha256 = digest(fileSha256, "File SHA-256");
            status = member(status, STATUSES, "Import status");
            positive(revision, "Import revision");
            if (previewDigest != null) {
                previewDigest = digest(previewDigest, "Preview digest");
            }
            count(totalRows);
            count(readyRows);
            count(invalidRows);
            count(importedRows);
            count(failedRows);
            failureCode = DtoValidation.optionalText(
                    failureCode,
                    "Import failure",
                    80);
            expiresAt = DtoValidation.requireInstant(
                    expiresAt,
                    "Import expiry");
            startedAt = optionalInstant(startedAt, "Import start");
            completedAt = optionalInstant(completedAt, "Import completion");
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Import creation");
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Import update");
            if (rows == null
                    || rows.size() > 100
                    || rows.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Catalog import rows are invalid.");
            }
            rows = List.copyOf(rows);
            nextCursor = DtoValidation.optionalText(
                    nextCursor,
                    "Import cursor",
                    10);
        }

        @Override
        public List<Row> rows() {
            return List.copyOf(rows);
        }
    }

    private static int positive(int value, String label) {
        if (value < MINIMUM_POSITIVE) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static void count(int value) {
        if (value < 0 || value > 25_000) {
            throw new IllegalArgumentException(
                    "Catalog import count is invalid.");
        }
    }

    private static String digest(String value, String label) {
        String result = DtoValidation.requireText(value, label, 64);
        if (!result.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return result;
    }

    private static String member(
            String value,
            Set<String> allowed,
            String label) {
        String result = DtoValidation.requireText(value, label, 40);
        if (!allowed.contains(result)) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return result;
    }

    private static String optionalInstant(String value, String label) {
        return value == null ? null : DtoValidation.requireInstant(value, label);
    }
}
