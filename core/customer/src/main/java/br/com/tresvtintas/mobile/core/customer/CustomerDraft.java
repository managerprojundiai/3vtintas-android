package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;

public record CustomerDraft(
        String name,
        Optional<String> email,
        Optional<String> phone,
        Optional<String> cpf,
        Optional<String> address,
        Optional<String> city,
        Optional<String> state,
        Optional<String> notes) {
    public CustomerDraft {
        name = CustomerValues.requiredText(name, "Customer name", 200);
        email = value(email);
        phone = value(phone);
        cpf = value(cpf);
        address = value(address);
        city = value(city);
        state = value(state)
                .map(String::toUpperCase);
        notes = value(notes);
        email.ifPresent(value -> {
            if (value.length() > 320 || !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Customer email is invalid.");
            }
        });
        phone.ifPresent(value -> validateMaximum(value, "Customer phone", 20));
        cpf.ifPresent(value -> validateMaximum(value, "Customer CPF", 14));
        address.ifPresent(value -> validateMaximum(value, "Customer address", 2_000));
        city.ifPresent(value -> validateMaximum(value, "Customer city", 100));
        state.ifPresent(value -> validateMaximum(value, "Customer state", 2));
        notes.ifPresent(value -> validateMaximum(value, "Customer notes", 4_000));
    }

    public static CustomerDraft fromRaw(
            String name,
            String email,
            String phone,
            String cpf,
            String address,
            String city,
            String state,
            String notes) {
        return new CustomerDraft(
                name,
                CustomerValues.optionalText(email, "Customer email", 320),
                CustomerValues.optionalText(phone, "Customer phone", 20),
                CustomerValues.optionalText(cpf, "Customer CPF", 14),
                CustomerValues.optionalText(address, "Customer address", 2_000),
                CustomerValues.optionalText(city, "Customer city", 100),
                CustomerValues.optionalText(state, "Customer state", 2),
                CustomerValues.optionalText(notes, "Customer notes", 4_000));
    }

    private static Optional<String> value(Optional<String> input) {
        return input == null ? Optional.empty() : input;
    }

    private static void validateMaximum(
            String value,
            String fieldName,
            int maximumLength) {
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }
}
