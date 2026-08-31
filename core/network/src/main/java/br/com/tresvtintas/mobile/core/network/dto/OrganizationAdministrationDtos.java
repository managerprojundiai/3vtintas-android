package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public final class OrganizationAdministrationDtos {
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> STATUSES = Set.of(
            "pending",
            "active",
            "blocked");

    private OrganizationAdministrationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsStatus(String status) {
        return STATUSES.contains(status);
    }

    public record Organization(
            long id,
            String slug,
            String name,
            String status,
            int revision,
            int activeMemberCount,
            int managerCount,
            int salespersonCount,
            int deliveryDriverCount,
            String createdAt,
            String updatedAt) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Organization ID");
            slug = DtoValidation.requireText(slug, "Organization slug", 120);
            name = DtoValidation.requireText(name, "Organization name", 200);
            requireStatus(status);
            requireRevision(revision);
            requireCount(activeMemberCount);
            requireCount(managerCount);
            requireCount(salespersonCount);
            requireCount(deliveryDriverCount);
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Organization creation");
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Organization update");
        }
    }

    public record Page(List<Organization> items, String nextCursor) {
        public Page {
            if (items == null
                    || items.size() > 100
                    || items.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException(
                        "Organization page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = DtoValidation.optionalText(
                    nextCursor,
                    "Organization cursor",
                    256);
        }

        @Override
        public List<Organization> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(long resourceId, int revision, boolean changed) {
        public Mutation {
            resourceId = DtoValidation.requirePositive(
                    resourceId,
                    "Organization mutation resource ID");
            requireRevision(revision);
        }
    }

    public record CreateRequest(String name, String slug, boolean confirmed) {
        public CreateRequest {
            name = DtoValidation.requireText(name, "Organization name", 200);
            slug = DtoValidation.requireText(slug, "Organization slug", 120);
            if (!confirmed) {
                throw new IllegalArgumentException(
                        "Organization creation is not confirmed.");
            }
        }
    }

    public record RenameRequest(
            String action,
            int expectedRevision,
            String name,
            boolean confirmed) {
        public RenameRequest {
            requireCommand(action, "rename", expectedRevision, confirmed);
            name = DtoValidation.requireText(name, "Organization name", 200);
        }
    }

    public record StatusRequest(
            String action,
            int expectedRevision,
            String status,
            boolean confirmed) {
        public StatusRequest {
            requireCommand(action, "status", expectedRevision, confirmed);
            if (!"active".equals(status) && !"blocked".equals(status)) {
                throw new IllegalArgumentException(
                        "Organization target status is invalid.");
            }
        }
    }

    private static void requireCommand(
            String action,
            String expected,
            int revision,
            boolean confirmed) {
        if (!expected.equals(action)
                || revision < MINIMUM_REVISION
                || !confirmed) {
            throw new IllegalArgumentException(
                    "Organization administration command is invalid.");
        }
    }

    private static void requireStatus(String status) {
        if (!supportsStatus(status)) {
            throw new IllegalArgumentException(
                    "Organization status is invalid.");
        }
    }

    private static void requireRevision(int value) {
        if (value < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Organization revision is invalid.");
        }
    }

    private static void requireCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Organization count is invalid.");
        }
    }
}
