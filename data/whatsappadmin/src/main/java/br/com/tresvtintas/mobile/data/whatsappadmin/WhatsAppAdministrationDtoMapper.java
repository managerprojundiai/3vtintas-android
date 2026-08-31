package br.com.tresvtintas.mobile.data.whatsappadmin;

import br.com.tresvtintas.mobile.core.network.dto.WhatsAppAdministrationDtos;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Connection;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Store;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppConnectionStatus;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppProvider;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppStoreStatus;
import java.time.Instant;
import java.util.Optional;

final class WhatsAppAdministrationDtoMapper {
    private static final String REQUEST_QR_ACTION = "request_evolution_qr";

    private WhatsAppAdministrationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Snapshot snapshot(WhatsAppAdministrationDtos.Snapshot dto) {
        return new Snapshot(dto.stores().stream()
                .map(WhatsAppAdministrationDtoMapper::store)
                .toList());
    }

    static ActionResult action(
            WhatsAppAdministrationDtos.ActionResult dto,
            boolean replayed) {
        if (REQUEST_QR_ACTION.equals(dto.action())
                || dto.qrCode() != null
                || dto.pairingCode() != null) {
            throw new IllegalArgumentException(
                    "Mutation response contains ephemeral credentials.");
        }
        return new ActionResult(
                dto.action(),
                dto.organizationId(),
                Optional.ofNullable(dto.connectionId()),
                optionalMode(dto.mode()),
                optionalStatus(dto.status()),
                Optional.ofNullable(dto.phoneNumber()),
                replayed);
    }

    static EphemeralQr qr(WhatsAppAdministrationDtos.ActionResult dto) {
        if (!REQUEST_QR_ACTION.equals(dto.action()) || dto.connectionId() == null) {
            throw new IllegalArgumentException("QR response is invalid.");
        }
        return new EphemeralQr(
                dto.organizationId(),
                dto.connectionId(),
                Optional.ofNullable(dto.qrCode()),
                Optional.ofNullable(dto.pairingCode()));
    }

    static WhatsAppAdministrationDtos.ConfigureMetaRequest configureMetaRequest(
            String phoneNumberId,
            Optional<String> phoneNumber) {
        return new WhatsAppAdministrationDtos.ConfigureMetaRequest(
                "configure_meta",
                phoneNumberId,
                phoneNumber.orElse(null),
                true);
    }

    static WhatsAppAdministrationDtos.ProvisionEvolutionRequest
            provisionEvolutionRequest() {
        return new WhatsAppAdministrationDtos.ProvisionEvolutionRequest(
                "provision_evolution",
                true);
    }

    static WhatsAppAdministrationDtos.SetModeRequest setModeRequest(
            WhatsAppChannelMode mode) {
        return new WhatsAppAdministrationDtos.SetModeRequest(
                "set_mode",
                mode.wireValue(),
                true);
    }

    static WhatsAppAdministrationDtos.RefreshEvolutionRequest
            refreshEvolutionRequest() {
        return new WhatsAppAdministrationDtos.RefreshEvolutionRequest(
                "refresh_evolution",
                true);
    }

    static WhatsAppAdministrationDtos.QrRequest qrRequest() {
        return new WhatsAppAdministrationDtos.QrRequest(true);
    }

    private static Store store(WhatsAppAdministrationDtos.Store dto) {
        return new Store(
                dto.id(),
                dto.slug(),
                dto.name(),
                WhatsAppStoreStatus.fromWireValue(dto.status()),
                WhatsAppChannelMode.fromWireValue(dto.mode()),
                dto.connections().stream()
                        .map(WhatsAppAdministrationDtoMapper::connection)
                        .toList());
    }

    private static Connection connection(
            WhatsAppAdministrationDtos.Connection dto) {
        return new Connection(
                dto.id(),
                dto.organizationId(),
                WhatsAppProvider.fromWireValue(dto.provider()),
                dto.label(),
                WhatsAppConnectionStatus.fromWireValue(dto.status()),
                dto.inboundEnabled(),
                dto.outboundEnabled(),
                Optional.ofNullable(dto.phoneNumber()),
                Optional.ofNullable(dto.phoneNumberId()),
                Optional.ofNullable(dto.evolutionInstanceName()),
                dto.hasError(),
                optionalInstant(dto.lastConnectedAt()),
                optionalInstant(dto.lastSeenAt()),
                Instant.parse(dto.updatedAt()));
    }

    private static Optional<WhatsAppChannelMode> optionalMode(String value) {
        return value == null
                ? Optional.empty()
                : Optional.of(WhatsAppChannelMode.fromWireValue(value));
    }

    private static Optional<WhatsAppConnectionStatus> optionalStatus(
            String value) {
        return value == null
                ? Optional.empty()
                : Optional.of(WhatsAppConnectionStatus.fromWireValue(value));
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
    }
}
