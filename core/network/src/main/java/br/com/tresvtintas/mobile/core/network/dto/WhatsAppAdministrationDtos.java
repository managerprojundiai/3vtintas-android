package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public final class WhatsAppAdministrationDtos {
    private static final int MINIMUM_ID = 1;
    private static final Set<String> STORE_STATUSES = Set.of(
            "pending", "active", "blocked");
    private static final Set<String> MODES = Set.of(
            "disabled", "meta", "evolution", "both");
    private static final Set<String> PROVIDERS = Set.of(
            "meta_cloud", "evolution");
    private static final Set<String> CONNECTION_STATUSES = Set.of(
            "pending",
            "connecting",
            "connected",
            "disconnected",
            "error",
            "disabled");
    private static final Set<String> ACTIONS = Set.of(
            "configure_meta",
            "provision_evolution",
            "set_mode",
            "request_evolution_qr",
            "refresh_evolution");

    private WhatsAppAdministrationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsMode(String value) {
        return MODES.contains(value);
    }

    public record Connection(
            long id,
            long organizationId,
            String provider,
            String label,
            String status,
            boolean inboundEnabled,
            boolean outboundEnabled,
            String phoneNumber,
            String phoneNumberId,
            String evolutionInstanceName,
            boolean hasError,
            String lastConnectedAt,
            String lastSeenAt,
            String updatedAt) {
        public Connection {
            id = positive(id, "Connection");
            organizationId = positive(organizationId, "Organization");
            provider = member(provider, PROVIDERS, "Provider");
            label = requiredText(label, 160, "Connection label");
            status = member(status, CONNECTION_STATUSES, "Connection status");
            phoneNumber = nullableText(phoneNumber, 30, "Phone number");
            phoneNumberId = nullableText(phoneNumberId, 120, "Phone number ID");
            evolutionInstanceName = nullableText(
                    evolutionInstanceName,
                    180,
                    "Evolution instance");
            lastConnectedAt = nullableInstant(lastConnectedAt, "Last connection");
            lastSeenAt = nullableInstant(lastSeenAt, "Last seen");
            updatedAt = DtoValidation.requireInstant(updatedAt, "Connection update");
        }
    }

    public record Store(
            long id,
            String slug,
            String name,
            String status,
            String mode,
            List<Connection> connections) {
        public Store {
            id = positive(id, "Store");
            slug = requiredText(slug, 120, "Store slug");
            name = requiredText(name, 200, "Store name");
            status = member(status, STORE_STATUSES, "Store status");
            mode = requireMode(mode);
            connections = List.copyOf(requireList(connections));
        }
    }

    public record Snapshot(List<Store> stores) {
        public Snapshot {
            stores = List.copyOf(requireList(stores));
        }
    }

    public record ConfigureMetaRequest(
            String action,
            String phoneNumberId,
            String phoneNumber,
            boolean confirmed) {
        public ConfigureMetaRequest {
            action = exact(action, "configure_meta");
            phoneNumberId = requiredText(phoneNumberId, 120, "Phone number ID");
            phoneNumber = nullableText(phoneNumber, 15, "Phone number");
            confirmed = requireConfirmed(confirmed);
        }
    }

    public record ProvisionEvolutionRequest(String action, boolean confirmed) {
        public ProvisionEvolutionRequest {
            action = exact(action, "provision_evolution");
            confirmed = requireConfirmed(confirmed);
        }
    }

    public record SetModeRequest(
            String action,
            String mode,
            boolean confirmed) {
        public SetModeRequest {
            action = exact(action, "set_mode");
            mode = requireMode(mode);
            confirmed = requireConfirmed(confirmed);
        }
    }

    public record RefreshEvolutionRequest(String action, boolean confirmed) {
        public RefreshEvolutionRequest {
            action = exact(action, "refresh_evolution");
            confirmed = requireConfirmed(confirmed);
        }
    }

    public record QrRequest(boolean confirmed) {
        public QrRequest {
            confirmed = requireConfirmed(confirmed);
        }
    }

    public record ActionResult(
            String action,
            long organizationId,
            Long connectionId,
            String mode,
            String status,
            String phoneNumber,
            String qrCode,
            String pairingCode) {
        public ActionResult {
            action = member(action, ACTIONS, "WhatsApp action");
            organizationId = positive(organizationId, "Organization");
            if (connectionId != null) {
                positive(connectionId, "Connection");
            }
            mode = nullableMember(mode, MODES, "WhatsApp mode");
            status = nullableMember(
                    status,
                    CONNECTION_STATUSES,
                    "Connection status");
            phoneNumber = nullableText(phoneNumber, 30, "Phone number");
            qrCode = nullableText(qrCode, 700_030, "QR code");
            pairingCode = nullableText(pairingCode, 32, "Pairing code");
        }
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

    private static String nullableText(String value, int limit, String label) {
        if (value != null && value.length() > limit) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static String member(String value, Set<String> values, String label) {
        if (!values.contains(value)) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }

    private static String nullableMember(
            String value,
            Set<String> values,
            String label) {
        return value == null ? null : member(value, values, label);
    }

    private static String requireMode(String value) {
        if (!supportsMode(value)) {
            throw new IllegalArgumentException("WhatsApp mode is invalid.");
        }
        return value;
    }

    private static String exact(String value, String expected) {
        if (!expected.equals(value)) {
            throw new IllegalArgumentException("WhatsApp action is invalid.");
        }
        return value;
    }

    private static boolean requireConfirmed(boolean value) {
        if (!value) {
            throw new IllegalArgumentException("WhatsApp action is not confirmed.");
        }
        return true;
    }

    private static String nullableInstant(String value, String label) {
        return value == null ? null : DtoValidation.requireInstant(value, label);
    }

    private static <T> List<T> requireList(List<T> value) {
        if (value == null) {
            throw new IllegalArgumentException("WhatsApp list is invalid.");
        }
        return value;
    }
}
