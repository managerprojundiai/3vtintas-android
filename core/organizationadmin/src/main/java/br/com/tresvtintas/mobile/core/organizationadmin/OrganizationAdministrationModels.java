package br.com.tresvtintas.mobile.core.organizationadmin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class OrganizationAdministrationModels {
    private static final long MINIMUM_ID = 1L;
    private static final int MINIMUM_REVISION = 1;

    private OrganizationAdministrationModels() {
        throw new AssertionError("No instances.");
    }

    public record Organization(
            long id,
            String slug,
            String name,
            OrganizationAdministrationStatus status,
            int revision,
            int activeMemberCount,
            int managerCount,
            int salespersonCount,
            int deliveryDriverCount,
            Instant createdAt,
            Instant updatedAt) {
        public Organization {
            requireId(id);
            slug = requireText(slug, "Organization slug");
            name = requireText(name, "Organization name");
            Objects.requireNonNull(status, "Organization status is required.");
            requireRevision(revision);
            requireCount(activeMemberCount);
            requireCount(managerCount);
            requireCount(salespersonCount);
            requireCount(deliveryDriverCount);
            Objects.requireNonNull(createdAt, "Organization creation is required.");
            Objects.requireNonNull(updatedAt, "Organization update is required.");
            if (activeMemberCount
                    < managerCount + salespersonCount + deliveryDriverCount) {
                throw new IllegalArgumentException(
                        "Organization member counts are inconsistent.");
            }
        }
    }

    public record Page(List<Organization> items, Optional<String> nextCursor) {
        public Page {
            if (items == null || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Organization page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<Organization> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(
            long resourceId,
            int revision,
            boolean changed,
            boolean replayed) {
        public Mutation {
            requireId(resourceId);
            requireRevision(revision);
        }
    }

    public static boolean isMutableStatus(
            OrganizationAdministrationStatus status) {
        return status == OrganizationAdministrationStatus.ACTIVE
                || status == OrganizationAdministrationStatus.BLOCKED;
    }

    private static void requireId(long value) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException("Organization ID is invalid.");
        }
    }

    private static void requireRevision(int value) {
        if (value < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Revision is invalid.");
        }
    }

    private static void requireCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Member count is invalid.");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }
}
