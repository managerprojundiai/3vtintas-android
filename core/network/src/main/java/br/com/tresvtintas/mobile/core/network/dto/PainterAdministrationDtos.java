package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PainterAdministrationDtos {
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> STATUSES =
            Set.of("pending", "active", "blocked");

    private PainterAdministrationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsStatus(String status) {
        return STATUSES.contains(status);
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Painter organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Painter organization name",
                    200);
        }
    }

    public record Manager(long id, String name, Long organizationId) {
        public Manager {
            id = DtoValidation.requirePositive(id, "Painter manager ID");
            name = DtoValidation.requireText(name, "Painter manager name", 200);
            if (organizationId != null) {
                organizationId = DtoValidation.requirePositive(
                        organizationId,
                        "Painter manager organization ID");
            }
        }
    }

    public record Options(
            List<Organization> organizations,
            List<Manager> managers) {
        public Options {
            organizations = copy(organizations, 500, "Painter organizations");
            managers = copy(managers, 2_000, "Painter managers");
        }

        @Override
        public List<Organization> organizations() {
            return List.copyOf(organizations);
        }

        @Override
        public List<Manager> managers() {
            return List.copyOf(managers);
        }
    }

    public record PainterSummary(
            long id,
            long userId,
            String name,
            String email,
            String company,
            String specialty,
            String status,
            String commissionRate,
            Organization organization,
            Manager manager,
            int revision,
            String updatedAt) {
        public PainterSummary {
            id = DtoValidation.requirePositive(id, "Painter ID");
            userId = DtoValidation.requirePositive(userId, "Painter user ID");
            name = DtoValidation.requireText(name, "Painter name", 200);
            email = DtoValidation.optionalText(email, "Painter email", 320);
            company = DtoValidation.optionalText(company, "Painter company", 200);
            specialty = DtoValidation.optionalText(
                    specialty,
                    "Painter specialty",
                    200);
            requireStatus(status);
            requireRate(commissionRate);
            Objects.requireNonNull(organization, "Painter organization is required.");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException("Painter revision is invalid.");
            }
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Painter update");
        }
    }

    public record PainterPage(
            List<PainterSummary> items,
            String nextCursor) {
        public PainterPage {
            items = copy(items, 100, "Painters");
            nextCursor = DtoValidation.optionalText(
                    nextCursor,
                    "Painter cursor",
                    256);
        }

        @Override
        public List<PainterSummary> items() {
            return List.copyOf(items);
        }
    }

    public record PainterDetail(
            long id,
            long userId,
            String name,
            String email,
            String company,
            String specialty,
            String status,
            String commissionRate,
            Organization organization,
            Manager manager,
            int revision,
            String updatedAt,
            String cpf,
            String rg,
            String phone,
            String whatsappPhone,
            String serviceArea,
            String address,
            String city,
            String state,
            String notes) {
        public PainterDetail {
            new PainterSummary(
                    id,
                    userId,
                    name,
                    email,
                    company,
                    specialty,
                    status,
                    commissionRate,
                    organization,
                    manager,
                    revision,
                    updatedAt);
            cpf = DtoValidation.optionalText(cpf, "Painter CPF", 14);
            rg = DtoValidation.optionalText(rg, "Painter RG", 20);
            phone = DtoValidation.optionalText(phone, "Painter phone", 20);
            whatsappPhone = DtoValidation.optionalText(
                    whatsappPhone,
                    "Painter WhatsApp",
                    20);
            serviceArea = DtoValidation.optionalText(
                    serviceArea,
                    "Painter service area",
                    2_000);
            address = DtoValidation.optionalText(address, "Painter address", 2_000);
            city = DtoValidation.optionalText(city, "Painter city", 100);
            state = DtoValidation.optionalText(state, "Painter state", 2);
            notes = DtoValidation.optionalText(notes, "Painter notes", 4_000);
        }
    }

    public record AccessRequestSummary(
            long id,
            long userId,
            String name,
            String email,
            String company,
            String specialty,
            String status,
            int revision,
            String createdAt,
            String updatedAt) {
        public AccessRequestSummary {
            id = DtoValidation.requirePositive(id, "Access request ID");
            userId = DtoValidation.requirePositive(
                    userId,
                    "Access request user ID");
            name = DtoValidation.requireText(name, "Access request name", 200);
            email = DtoValidation.optionalText(
                    email,
                    "Access request email",
                    320);
            company = DtoValidation.optionalText(
                    company,
                    "Access request company",
                    200);
            specialty = DtoValidation.optionalText(
                    specialty,
                    "Access request specialty",
                    200);
            if (!"pending".equals(status)
                    || revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException(
                        "Access request state is invalid.");
            }
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Access request creation");
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Access request update");
        }
    }

    public record AccessRequestPage(
            List<AccessRequestSummary> items,
            String nextCursor) {
        public AccessRequestPage {
            items = copy(items, 100, "Access requests");
            nextCursor = DtoValidation.optionalText(
                    nextCursor,
                    "Access request cursor",
                    256);
        }

        @Override
        public List<AccessRequestSummary> items() {
            return List.copyOf(items);
        }
    }

    public record AccessRequestDetail(
            long id,
            long userId,
            String name,
            String email,
            String company,
            String specialty,
            String status,
            int revision,
            String createdAt,
            String updatedAt,
            String cpf,
            String phone,
            String whatsappPhone) {
        public AccessRequestDetail {
            new AccessRequestSummary(
                    id,
                    userId,
                    name,
                    email,
                    company,
                    specialty,
                    status,
                    revision,
                    createdAt,
                    updatedAt);
            cpf = DtoValidation.optionalText(cpf, "Access request CPF", 14);
            phone = DtoValidation.requireText(
                    phone,
                    "Access request phone",
                    20);
            whatsappPhone = DtoValidation.optionalText(
                    whatsappPhone,
                    "Access request WhatsApp",
                    20);
        }
    }

    public record Mutation(long resourceId, int revision, boolean changed) {
        public Mutation {
            resourceId = DtoValidation.requirePositive(
                    resourceId,
                    "Painter mutation resource ID");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException(
                        "Painter mutation revision is invalid.");
            }
        }
    }

    public record CreateRequest(
            long organizationId,
            String name,
            String email,
            String cpf,
            String rg,
            String phone,
            String company,
            String specialty,
            String serviceArea,
            String address,
            String city,
            String state,
            double commissionRate,
            Long managerUserId,
            String notes,
            boolean confirmed) {
        public CreateRequest {
            if (organizationId < 1
                    || name == null
                    || name.strip().length() < 2
                    || email == null
                    || email.isBlank()
                    || commissionRate < 0
                    || commissionRate > 100
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Painter create request is invalid.");
            }
        }
    }

    public record StatusRequest(
            String action,
            int expectedRevision,
            String status,
            boolean confirmed) {
        public StatusRequest {
            if (!"status".equals(action)
                    || expectedRevision < MINIMUM_REVISION
                    || !STATUSES.contains(status)
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Painter status request is invalid.");
            }
        }
    }

    public record CommissionRequest(
            String action,
            int expectedRevision,
            double commissionRate,
            boolean confirmed) {
        public CommissionRequest {
            if (!"commission".equals(action)
                    || expectedRevision < MINIMUM_REVISION
                    || commissionRate < 0
                    || commissionRate > 100
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Painter commission request is invalid.");
            }
        }
    }

    public record ManagerRequest(
            String action,
            int expectedRevision,
            Long managerUserId,
            boolean confirmed) {
        public ManagerRequest {
            if (!"manager".equals(action)
                    || expectedRevision < MINIMUM_REVISION
                    || managerUserId != null && managerUserId < 1
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Painter manager request is invalid.");
            }
        }
    }

    public record PainterDecisionRequest(
            String decision,
            int expectedRevision,
            long organizationId,
            Long managerUserId,
            double commissionRate,
            boolean confirmed) {
        public PainterDecisionRequest {
            if (!"painter".equals(decision)
                    || expectedRevision < MINIMUM_REVISION
                    || organizationId < 1
                    || managerUserId != null && managerUserId < 1
                    || commissionRate < 0
                    || commissionRate > 100
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Painter approval request is invalid.");
            }
        }
    }

    public record ManagerDecisionRequest(
            String decision,
            int expectedRevision,
            long organizationId,
            boolean confirmed) {
        public ManagerDecisionRequest {
            if (!"manager".equals(decision)
                    || expectedRevision < MINIMUM_REVISION
                    || organizationId < 1
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Manager approval request is invalid.");
            }
        }
    }

    public record RejectDecisionRequest(
            String decision,
            int expectedRevision,
            String reason,
            boolean confirmed) {
        public RejectDecisionRequest {
            if (!"reject".equals(decision)
                    || expectedRevision < MINIMUM_REVISION
                    || reason == null
                    || reason.strip().length() < 3
                    || reason.strip().length() > 500
                    || !confirmed) {
                throw new IllegalArgumentException(
                        "Access rejection request is invalid.");
            }
        }
    }

    private static void requireStatus(String status) {
        if (!supportsStatus(status)) {
            throw new IllegalArgumentException("Painter status is invalid.");
        }
    }

    private static void requireRate(String rate) {
        if (rate == null || !rate.matches("^\\d{1,3}\\.\\d{2}$")) {
            throw new IllegalArgumentException("Painter rate is invalid.");
        }
    }

    private static <T> List<T> copy(
            List<T> values,
            int maximum,
            String label) {
        if (values == null
                || values.size() > maximum
                || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }
}
