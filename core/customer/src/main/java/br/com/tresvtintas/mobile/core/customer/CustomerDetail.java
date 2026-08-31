package br.com.tresvtintas.mobile.core.customer;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record CustomerDetail(
        long id,
        OptionalLong userId,
        OptionalLong organizationId,
        String name,
        Optional<String> email,
        Optional<String> phone,
        Optional<String> cpf,
        Optional<String> address,
        Optional<String> city,
        Optional<String> state,
        Optional<String> notes,
        OptionalLong assignedSalespersonUserId,
        Instant createdAt,
        Instant updatedAt) {
    public CustomerDetail {
        id = CustomerValues.positiveId(id, "Customer ID");
        userId = validateId(userId, "Customer user ID");
        organizationId = validateId(organizationId, "Customer organization ID");
        name = CustomerValues.requiredText(name, "Customer name", 200);
        email = value(email);
        phone = value(phone);
        cpf = value(cpf);
        address = value(address);
        city = value(city);
        state = value(state);
        notes = value(notes);
        assignedSalespersonUserId =
                validateId(assignedSalespersonUserId, "Assigned salesperson ID");
        Objects.requireNonNull(createdAt, "Customer creation time is required.");
        Objects.requireNonNull(updatedAt, "Customer update time is required.");
    }

    public CustomerDraft toDraft() {
        return new CustomerDraft(
                name,
                email,
                phone,
                cpf,
                address,
                city,
                state,
                notes);
    }

    private static Optional<String> value(Optional<String> input) {
        return input == null ? Optional.empty() : input;
    }

    private static OptionalLong validateId(
            OptionalLong input,
            String fieldName) {
        OptionalLong normalized = input == null ? OptionalLong.empty() : input;
        if (normalized.isPresent()) {
            CustomerValues.positiveId(normalized.getAsLong(), fieldName);
        }
        return normalized;
    }
}
