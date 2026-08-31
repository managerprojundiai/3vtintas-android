package br.com.tresvtintas.mobile.core.painteradmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class PainterAdministrationModels {
    private static final long MINIMUM_ID = 1L;
    private static final int MINIMUM_REVISION = 1;

    private PainterAdministrationModels() {
        throw new AssertionError("No instances.");
    }

    public record Organization(long id, String name) {
        public Organization {
            requireId(id, "Organization");
            name = requireText(name, "Organization name");
        }
    }

    public record Manager(
            long id,
            String name,
            OptionalLong organizationId) {
        public Manager {
            requireId(id, "Manager");
            name = requireText(name, "Manager name");
            organizationId = organizationId == null
                    ? OptionalLong.empty()
                    : organizationId;
            if (organizationId.isPresent()) {
                requireId(organizationId.orElseThrow(), "Manager organization");
            }
        }
    }

    public record Options(
            List<Organization> organizations,
            List<Manager> managers) {
        public Options {
            organizations = copy(organizations, "Organizations");
            managers = copy(managers, "Managers");
        }

        @Override
        public List<Organization> organizations() {
            return List.copyOf(organizations);
        }

        @Override
        public List<Manager> managers() {
            return List.copyOf(managers);
        }

        public List<Manager> managersFor(long organizationId) {
            return managers.stream()
                    .filter(value -> value.organizationId().isPresent()
                            && value.organizationId().orElseThrow()
                                    == organizationId)
                    .toList();
        }
    }

    public record Painter(
            long id,
            long userId,
            String name,
            Optional<String> email,
            Optional<String> company,
            Optional<String> specialty,
            PainterAdministrationStatus status,
            BigDecimal commissionRate,
            Organization organization,
            Optional<Manager> manager,
            int revision,
            Instant updatedAt) {
        public Painter {
            requireId(id, "Painter");
            requireId(userId, "Painter user");
            name = requireText(name, "Painter name");
            email = optional(email);
            company = optional(company);
            specialty = optional(specialty);
            Objects.requireNonNull(status, "Painter status is required.");
            requireRate(commissionRate);
            Objects.requireNonNull(
                    organization,
                    "Painter organization is required.");
            manager = manager == null ? Optional.empty() : manager;
            requireRevision(revision);
            Objects.requireNonNull(updatedAt, "Painter update is required.");
        }
    }

    public record PainterDetail(
            Painter painter,
            Optional<String> cpf,
            Optional<String> rg,
            Optional<String> phone,
            Optional<String> whatsappPhone,
            Optional<String> serviceArea,
            Optional<String> address,
            Optional<String> city,
            Optional<String> state,
            Optional<String> notes) {
        public PainterDetail {
            Objects.requireNonNull(painter, "Painter is required.");
            cpf = optional(cpf);
            rg = optional(rg);
            phone = optional(phone);
            whatsappPhone = optional(whatsappPhone);
            serviceArea = optional(serviceArea);
            address = optional(address);
            city = optional(city);
            state = optional(state);
            notes = optional(notes);
        }
    }

    public record AccessRequest(
            long id,
            long userId,
            String name,
            Optional<String> email,
            Optional<String> company,
            Optional<String> specialty,
            int revision,
            Instant createdAt,
            Instant updatedAt) {
        public AccessRequest {
            requireId(id, "Access request");
            requireId(userId, "Access request user");
            name = requireText(name, "Access request name");
            email = optional(email);
            company = optional(company);
            specialty = optional(specialty);
            requireRevision(revision);
            Objects.requireNonNull(
                    createdAt,
                    "Access request creation is required.");
            Objects.requireNonNull(
                    updatedAt,
                    "Access request update is required.");
        }
    }

    public record AccessRequestDetail(
            AccessRequest request,
            Optional<String> cpf,
            String phone,
            Optional<String> whatsappPhone) {
        public AccessRequestDetail {
            Objects.requireNonNull(request, "Access request is required.");
            cpf = optional(cpf);
            phone = requireText(phone, "Access request phone");
            whatsappPhone = optional(whatsappPhone);
        }
    }

    public record Page<T>(List<T> items, Optional<String> nextCursor) {
        public Page {
            items = copy(items, "Page items");
            nextCursor = optional(nextCursor);
        }

        @Override
        public List<T> items() {
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

    static void requireRate(BigDecimal rate) {
        if (rate == null
                || rate.scale() > 2
                || rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Commission rate is invalid.");
        }
    }

    static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }

    static Optional<String> optional(Optional<String> value) {
        return value == null ? Optional.empty() : value
                .map(String::strip)
                .filter(item -> !item.isEmpty());
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

    private static <T> List<T> copy(List<T> values, String label) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }
}
