package br.com.tresvtintas.mobile.core.catalogadmin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CatalogImportModels {
    public static final int MAXIMUM_FILE_BYTES = 5 * 1024 * 1024;
    public static final int MAXIMUM_ROWS = 25_000;
    private static final int MINIMUM_POSITIVE = 1;
    private static final int SHA_256_HEX_LENGTH = 64;

    private CatalogImportModels() {
    }

    public enum Status {
        UPLOADED,
        PREPARING,
        PREVIEW_READY,
        QUEUED,
        RUNNING,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED,
        EXPIRED;

        public boolean terminal() {
            return this == COMPLETED
                    || this == COMPLETED_WITH_ERRORS
                    || this == FAILED
                    || this == EXPIRED;
        }
    }

    public enum Action {
        CREATE,
        UPDATE,
        SKIP
    }

    public enum RowStatus {
        READY,
        INVALID,
        RUNNING,
        IMPORTED,
        FAILED
    }

    public record SourceFile(
            String name,
            String mediaType,
            String sha256,
            byte[] bytes) {
        public SourceFile {
            name = required(name, "File name", 255);
            mediaType = switch (required(mediaType, "Media type", 100)) {
                case "text/csv", "application/csv",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    mediaType;
                default -> throw new IllegalArgumentException(
                        "Catalog import media type is invalid.");
            };
            sha256 = required(sha256, "SHA-256", SHA_256_HEX_LENGTH);
            if (!sha256.matches("^[a-f0-9]{64}$")) {
                throw new IllegalArgumentException(
                        "Catalog import SHA-256 is invalid.");
            }
            if (bytes == null
                    || bytes.length == 0
                    || bytes.length > MAXIMUM_FILE_BYTES) {
                throw new IllegalArgumentException(
                        "Catalog import file size is invalid.");
            }
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        public int byteCount() {
            return bytes.length;
        }
    }

    public record Mutation(
            String importId,
            int revision,
            Status status,
            boolean replayed) {
        public Mutation {
            importId = uuid(importId, "Import ID");
            if (revision < MINIMUM_POSITIVE) {
                throw new IllegalArgumentException(
                        "Catalog import revision is invalid.");
            }
            Objects.requireNonNull(status, "Catalog import status is required.");
        }
    }

    public record Preview(
            String name,
            Optional<String> sku,
            Optional<String> price,
            Optional<Integer> stock,
            Optional<String> categoryName) {
        public Preview {
            name = required(name, "Product name", 200);
            sku = optional(sku, "SKU", 50);
            price = optional(price, "Price", 11);
            stock = stock == null ? Optional.empty() : stock;
            categoryName = optional(categoryName, "Category", 100);
            stock.ifPresent(value -> {
                if (value < 0 || value > 2_000_000_000) {
                    throw new IllegalArgumentException(
                            "Catalog import stock is invalid.");
                }
            });
        }
    }

    public record Row(
            int rowNumber,
            Action action,
            RowStatus status,
            Preview preview,
            Optional<String> message) {
        public Row {
            if (rowNumber < MINIMUM_POSITIVE) {
                throw new IllegalArgumentException(
                        "Catalog import row number is invalid.");
            }
            Objects.requireNonNull(action, "Catalog import action is required.");
            Objects.requireNonNull(status, "Catalog import row status is required.");
            Objects.requireNonNull(preview, "Catalog import preview is required.");
            message = optional(message, "Import row message", 2_000);
        }
    }

    public record Batch(
            String id,
            String fileName,
            Status status,
            int revision,
            Optional<String> previewDigest,
            int totalRows,
            int readyRows,
            int invalidRows,
            int importedRows,
            int failedRows,
            Optional<String> failureCode,
            Instant expiresAt,
            List<Row> rows,
            Optional<String> nextCursor) {
        public Batch {
            id = uuid(id, "Import ID");
            fileName = required(fileName, "File name", 255);
            Objects.requireNonNull(status, "Catalog import status is required.");
            if (revision < MINIMUM_POSITIVE) {
                throw new IllegalArgumentException(
                        "Catalog import revision is invalid.");
            }
            previewDigest = optional(previewDigest, "Preview digest", 64);
            previewDigest.ifPresent(value -> {
                if (!value.matches("^[a-f0-9]{64}$")) {
                    throw new IllegalArgumentException(
                            "Catalog preview digest is invalid.");
                }
            });
            validateCount(totalRows);
            validateCount(readyRows);
            validateCount(invalidRows);
            validateCount(importedRows);
            validateCount(failedRows);
            failureCode = optional(failureCode, "Failure code", 80);
            Objects.requireNonNull(expiresAt, "Catalog import expiry is required.");
            rows = rows == null ? List.of() : List.copyOf(rows);
            nextCursor = optional(nextCursor, "Import cursor", 10);
        }

        @Override
        public List<Row> rows() {
            return List.copyOf(rows);
        }

        public boolean confirmable() {
            return status == Status.PREVIEW_READY
                    && readyRows > 0
                    && previewDigest.isPresent();
        }
    }

    private static void validateCount(int value) {
        if (value < 0 || value > MAXIMUM_ROWS) {
            throw new IllegalArgumentException(
                    "Catalog import count is invalid.");
        }
    }

    private static Optional<String> optional(
            Optional<String> value,
            String label,
            int maximum) {
        Optional<String> safe = value == null ? Optional.empty() : value;
        return safe.map(item -> required(item, label, maximum));
    }

    private static String required(
            String value,
            String label,
            int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static String uuid(String value, String label) {
        String normalized = required(value, label, 36);
        if (!normalized.matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return normalized;
    }
}
