package br.com.tresvtintas.mobile.core.customer;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record CustomerSummary(
        long id,
        OptionalLong organizationId,
        String name,
        Optional<String> email,
        Optional<String> phone,
        Optional<String> city,
        Optional<String> state,
        OptionalLong assignedSalespersonUserId,
        Instant createdAt,
        Instant updatedAt) {
    public CustomerSummary {
        id = CustomerValues.positiveId(id, "Customer ID");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (organizationId.isPresent()) {
            CustomerValues.positiveId(
                    organizationId.getAsLong(),
                    "Customer organization ID");
        }
        name = CustomerValues.requiredText(name, "Customer name", 200);
        email = email == null ? Optional.empty() : email;
        phone = phone == null ? Optional.empty() : phone;
        city = city == null ? Optional.empty() : city;
        state = state == null ? Optional.empty() : state;
        assignedSalespersonUserId = assignedSalespersonUserId == null
                ? OptionalLong.empty()
                : assignedSalespersonUserId;
        if (assignedSalespersonUserId.isPresent()) {
            CustomerValues.positiveId(
                    assignedSalespersonUserId.getAsLong(),
                    "Assigned salesperson ID");
        }
        Objects.requireNonNull(createdAt, "Customer creation time is required.");
        Objects.requireNonNull(updatedAt, "Customer update time is required.");
    }
}
