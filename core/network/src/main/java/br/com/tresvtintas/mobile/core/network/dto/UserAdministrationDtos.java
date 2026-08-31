package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public final class UserAdministrationDtos {
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> ROLES = Set.of(
            "master_admin",
            "manager",
            "salesperson",
            "delivery_driver",
            "painter",
            "customer",
            "user");
    private static final Set<String> STANDARD_ROLES = Set.of(
            "manager",
            "painter",
            "customer",
            "user");
    private static final Set<String> OPERATIONAL_ROLES = Set.of(
            "salesperson",
            "delivery_driver");

    private UserAdministrationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsRole(String role) {
        return ROLES.contains(role);
    }

    public record Organization(long id, String name, String slug) {
        public Organization {
            id = DtoValidation.requirePositive(id, "User organization ID");
            name = DtoValidation.requireText(name, "User organization name", 200);
            slug = DtoValidation.requireText(slug, "User organization slug", 120);
        }
    }

    public record Assignment(
            long organizationId,
            String organizationName,
            String role,
            String approvedAt) {
        public Assignment {
            organizationId = DtoValidation.requirePositive(
                    organizationId,
                    "User assignment organization ID");
            organizationName = DtoValidation.requireText(
                    organizationName,
                    "User assignment organization name",
                    200);
            requireRole(role, OPERATIONAL_ROLES, "User assignment role");
            if (approvedAt != null) {
                approvedAt = DtoValidation.requireInstant(
                        approvedAt,
                        "User assignment approval");
            }
        }
    }

    public record Options(
            List<Organization> organizations,
            List<String> standardRoles,
            List<String> operationalRoles) {
        public Options {
            organizations = copy(organizations, 500, "User organizations");
            standardRoles = copy(standardRoles, 10, "User standard roles");
            operationalRoles = copy(
                    operationalRoles,
                    10,
                    "User operational roles");
            standardRoles.forEach(
                    role -> requireRole(role, STANDARD_ROLES, "User standard role"));
            operationalRoles.forEach(
                    role -> requireRole(role, OPERATIONAL_ROLES, "User operational role"));
        }

        @Override
        public List<Organization> organizations() {
            return List.copyOf(organizations);
        }

        @Override
        public List<String> standardRoles() {
            return List.copyOf(standardRoles);
        }

        @Override
        public List<String> operationalRoles() {
            return List.copyOf(operationalRoles);
        }
    }

    public record User(
            long id,
            String name,
            String email,
            String role,
            boolean isBlocked,
            int revision,
            List<Assignment> assignments,
            String createdAt,
            String updatedAt) {
        public User {
            id = DtoValidation.requirePositive(id, "User ID");
            name = DtoValidation.requireText(name, "User name", 200);
            email = DtoValidation.optionalText(email, "User email", 320);
            requireRole(role, ROLES, "User role");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException("User revision is invalid.");
            }
            assignments = copy(assignments, 100, "User assignments");
            createdAt = DtoValidation.requireInstant(createdAt, "User creation");
            updatedAt = DtoValidation.requireInstant(updatedAt, "User update");
        }

        @Override
        public List<Assignment> assignments() {
            return List.copyOf(assignments);
        }
    }

    public record Page(List<User> items, String nextCursor) {
        public Page {
            items = copy(items, 100, "Users");
            nextCursor = DtoValidation.optionalText(nextCursor, "User cursor", 256);
        }

        @Override
        public List<User> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(long resourceId, int revision, boolean changed) {
        public Mutation {
            resourceId = DtoValidation.requirePositive(
                    resourceId,
                    "User mutation resource ID");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException("User mutation revision is invalid.");
            }
        }
    }

    public record StandardRoleRequest(
            String action,
            int expectedRevision,
            String role,
            boolean confirmed) {
        public StandardRoleRequest {
            requireCommand(action, "standard_role", expectedRevision, confirmed);
            requireRole(role, STANDARD_ROLES, "User standard role");
        }
    }

    public record OperationalRoleRequest(
            String action,
            int expectedRevision,
            String role,
            long organizationId,
            boolean confirmed) {
        public OperationalRoleRequest {
            requireCommand(action, "operational_role", expectedRevision, confirmed);
            requireRole(role, OPERATIONAL_ROLES, "User operational role");
            organizationId = DtoValidation.requirePositive(
                    organizationId,
                    "User operational organization ID");
        }
    }

    public record AccountStatusRequest(
            String action,
            int expectedRevision,
            boolean isBlocked,
            boolean confirmed) {
        public AccountStatusRequest {
            requireCommand(action, "account_status", expectedRevision, confirmed);
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
            throw new IllegalArgumentException("User administration command is invalid.");
        }
    }

    private static void requireRole(String role, Set<String> supported, String label) {
        if (!supported.contains(role)) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
    }

    private static <T> List<T> copy(List<T> values, int maximum, String label) {
        if (values == null
                || values.size() > maximum
                || values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }
}
