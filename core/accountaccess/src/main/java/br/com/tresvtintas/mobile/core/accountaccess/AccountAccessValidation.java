package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Optional;
import java.util.UUID;

final class AccountAccessValidation {
    private AccountAccessValidation() {
        throw new AssertionError("No instances.");
    }

    static String text(
            String value,
            String fieldName,
            int maximumLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " is invalid.");
        }
        return value;
    }

    static Optional<String> optionalText(
            Optional<String> value,
            String fieldName,
            int maximumLength) {
        Optional<String> normalized =
                value == null ? Optional.empty() : value;
        normalized.ifPresent(item ->
                text(item, fieldName, maximumLength));
        return normalized;
    }

    static String uuid(String value, String fieldName) {
        text(value, fieldName, 36);
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    fieldName + " is not a UUID.",
                    exception);
        }
    }
}
