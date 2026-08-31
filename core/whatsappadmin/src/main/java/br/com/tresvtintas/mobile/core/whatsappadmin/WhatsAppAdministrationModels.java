package br.com.tresvtintas.mobile.core.whatsappadmin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class WhatsAppAdministrationModels {
    private static final int MINIMUM_ID = 1;
    private static final String PHONE_NUMBER_LABEL = "Phone number";
    private static final Pattern SLUG = Pattern.compile(
            "^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern PHONE_NUMBER_ID = Pattern.compile("^\\d{5,120}$");
    private static final Pattern PHONE_NUMBER = Pattern.compile("^\\d{10,15}$");

    private WhatsAppAdministrationModels() {
        throw new AssertionError("No instances.");
    }

    public record Connection(
            long id,
            long organizationId,
            WhatsAppProvider provider,
            String label,
            WhatsAppConnectionStatus status,
            boolean inboundEnabled,
            boolean outboundEnabled,
            Optional<String> phoneNumber,
            Optional<String> phoneNumberId,
            Optional<String> evolutionInstanceName,
            boolean hasError,
            Optional<Instant> lastConnectedAt,
            Optional<Instant> lastSeenAt,
            Instant updatedAt) {
        public Connection {
            id = positive(id, "Connection");
            organizationId = positive(organizationId, "Organization");
            Objects.requireNonNull(provider, "Provider is required.");
            label = requiredText(label, 160, "Connection label");
            Objects.requireNonNull(status, "Connection status is required.");
            phoneNumber = optionalText(phoneNumber, 30, PHONE_NUMBER_LABEL);
            phoneNumberId = optionalText(phoneNumberId, 120, "Phone number ID");
            evolutionInstanceName = optionalText(
                    evolutionInstanceName,
                    180,
                    "Evolution instance");
            lastConnectedAt = requiredOptional(lastConnectedAt, "Last connection");
            lastSeenAt = requiredOptional(lastSeenAt, "Last seen");
            Objects.requireNonNull(updatedAt, "Connection update is required.");
        }
    }

    public record Store(
            long id,
            String slug,
            String name,
            WhatsAppStoreStatus status,
            WhatsAppChannelMode mode,
            List<Connection> connections) {
        public Store {
            long normalizedId = positive(id, "Store");
            id = normalizedId;
            slug = requiredText(slug, 120, "Store slug");
            if (!SLUG.matcher(slug).matches()) {
                throw new IllegalArgumentException("Store slug is invalid.");
            }
            name = requiredText(name, 200, "Store name");
            Objects.requireNonNull(status, "Store status is required.");
            Objects.requireNonNull(mode, "WhatsApp mode is required.");
            connections = List.copyOf(Objects.requireNonNull(
                    connections,
                    "Connections are required."));
            if (connections.stream().anyMatch(
                    item -> item.organizationId() != normalizedId)) {
                throw new IllegalArgumentException(
                        "Connection does not belong to its store.");
            }
        }

        public Optional<Connection> connection(WhatsAppProvider provider) {
            Objects.requireNonNull(provider, "Provider is required.");
            return connections.stream()
                    .filter(item -> item.provider() == provider)
                    .findFirst();
        }
    }

    public record Snapshot(List<Store> stores) {
        public Snapshot {
            stores = List.copyOf(Objects.requireNonNull(stores, "Stores are required."));
        }

        public Optional<Store> store(long organizationId) {
            return stores.stream()
                    .filter(item -> item.id() == organizationId)
                    .findFirst();
        }
    }

    public record ActionResult(
            String action,
            long organizationId,
            Optional<Long> connectionId,
            Optional<WhatsAppChannelMode> mode,
            Optional<WhatsAppConnectionStatus> status,
            Optional<String> phoneNumber,
            boolean replayed) {
        public ActionResult {
            action = requiredText(action, 40, "Action");
            organizationId = positive(organizationId, "Organization");
            connectionId = requiredOptional(connectionId, "Connection ID");
            connectionId.ifPresent(value -> positive(value, "Connection"));
            mode = requiredOptional(mode, "Mode");
            status = requiredOptional(status, "Status");
            phoneNumber = optionalText(phoneNumber, 30, PHONE_NUMBER_LABEL);
        }
    }

    public record EphemeralQr(
            long organizationId,
            long connectionId,
            Optional<String> encodedImage,
            Optional<String> pairingCode) {
        public EphemeralQr {
            organizationId = positive(organizationId, "Organization");
            connectionId = positive(connectionId, "Connection");
            encodedImage = optionalText(encodedImage, 700_030, "QR code");
            pairingCode = optionalText(pairingCode, 32, "Pairing code");
            if (encodedImage.isEmpty() && pairingCode.isEmpty()) {
                throw new IllegalArgumentException(
                        "QR or pairing code is required.");
            }
        }
    }

    public static String phoneNumberId(String value) {
        return matching(value, PHONE_NUMBER_ID, "Phone number ID");
    }

    public static Optional<String> phoneNumber(Optional<String> value) {
        Optional<String> normalized = optionalText(
                value,
                15,
                PHONE_NUMBER_LABEL);
        normalized.ifPresent(item -> matching(
                item,
                PHONE_NUMBER,
                PHONE_NUMBER_LABEL));
        return normalized;
    }

    private static long positive(long value, String label) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException(label + " ID is invalid.");
        }
        return value;
    }

    private static String requiredText(String value, int limit, String label) {
        if (value == null || value.isBlank() || value.length() > limit) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            int limit,
            String label) {
        Optional<String> required = requiredOptional(value, label);
        if (required.isEmpty()) {
            return required;
        }
        String text = required.orElseThrow().strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (text.length() > limit) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return Optional.of(text);
    }

    private static <T> Optional<T> requiredOptional(
            Optional<T> value,
            String label) {
        return Objects.requireNonNull(value, label + " container is required.");
    }

    private static String matching(String value, Pattern pattern, String label) {
        if (value == null || !pattern.matcher(value.strip()).matches()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }
}
