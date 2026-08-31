package br.com.tresvtintas.mobile.core.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UserAdministrationModels {
    private static final long MINIMUM_ID = 1L;
    private static final int MINIMUM_REVISION = 1;
    private UserAdministrationModels() {
        throw new AssertionError("No instances.");
    }

    public record Organization(long id, String name, String slug) {
        public Organization {
            requireId(id, "Organization");
            name = requireText(name, "Organization name");
            slug = requireText(slug, "Organization slug");
        }
    }

    public record Assignment(
            long organizationId,
            String organizationName,
            AppRole role,
            Optional<Instant> approvedAt) {
        public Assignment {
            requireId(organizationId, "Assignment organization");
            organizationName = requireText(
                    organizationName,
                    "Assignment organization name");
            requireOperationalRole(role);
            approvedAt = approvedAt == null ? Optional.empty() : approvedAt;
        }
    }

    public record Options(
            List<Organization> organizations,
            List<AppRole> standardRoles,
            List<AppRole> operationalRoles) {
        public Options {
            organizations = copy(organizations, "Organizations");
            standardRoles = copy(standardRoles, "Standard roles");
            operationalRoles = copy(operationalRoles, "Operational roles");
            if (standardRoles.stream().anyMatch(UserAdministrationModels::isOperational)
                    || operationalRoles.stream().anyMatch(role -> !isOperational(role))
                    || standardRoles.contains(AppRole.MASTER_ADMIN)) {
                throw new IllegalArgumentException("User role options are invalid.");
            }
        }

        @Override
        public List<Organization> organizations() {
            return List.copyOf(organizations);
        }

        @Override
        public List<AppRole> standardRoles() {
            return List.copyOf(standardRoles);
        }

        @Override
        public List<AppRole> operationalRoles() {
            return List.copyOf(operationalRoles);
        }
    }

    public record User(
            long id,
            String name,
            Optional<String> email,
            AppRole role,
            boolean blocked,
            int revision,
            List<Assignment> assignments,
            Instant createdAt,
            Instant updatedAt) {
        public User {
            requireId(id, "User");
            name = requireText(name, "User name");
            email = email == null
                    ? Optional.empty()
                    : email.map(String::strip).filter(value -> !value.isEmpty());
            Objects.requireNonNull(role, "User role is required.");
            requireRevision(revision);
            assignments = copy(assignments, "User assignments");
            Objects.requireNonNull(createdAt, "User creation is required.");
            Objects.requireNonNull(updatedAt, "User update is required.");
        }

        @Override
        public List<Assignment> assignments() {
            return List.copyOf(assignments);
        }

        public UserAdministrationStatus status() {
            return blocked
                    ? UserAdministrationStatus.BLOCKED
                    : UserAdministrationStatus.ACTIVE;
        }

        public boolean protectedFromChanges(long actorUserId) {
            return id == actorUserId || role == AppRole.MASTER_ADMIN;
        }
    }

    public record Page(List<User> items, Optional<String> nextCursor) {
        public Page {
            items = copy(items, "Page items");
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<User> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(
            long resourceId,
            int revision,
            boolean changed,
            boolean replayed) {
        public Mutation {
            requireId(resourceId, "Mutation resource");
            requireRevision(revision);
        }
    }

    public static boolean isOperational(AppRole role) {
        return role == AppRole.SALESPERSON || role == AppRole.DELIVERY_DRIVER;
    }

    public static void requireOperationalRole(AppRole role) {
        if (!isOperational(role)) {
            throw new IllegalArgumentException("Operational role is invalid.");
        }
    }

    private static void requireId(long value, String label) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException(label + " ID is invalid.");
        }
    }

    private static void requireRevision(int value) {
        if (value < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Revision is invalid.");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }

    private static <T> List<T> copy(List<T> values, String label) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }
}
