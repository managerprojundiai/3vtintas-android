package br.com.tresvtintas.mobile.data.whatsappadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.dto.WhatsAppAdministrationDtos;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import java.util.List;
import org.junit.Test;

public final class WhatsAppAdministrationDtoMapperTest {
    @Test
    public void mapsSafeSnapshotAndMutation() {
        WhatsAppAdministrationDtos.Snapshot dto =
                new WhatsAppAdministrationDtos.Snapshot(List.of(
                        new WhatsAppAdministrationDtos.Store(
                                31L,
                                "jundiai",
                                "3V Tintas Jundiaí",
                                "active",
                                "both",
                                List.of(evolutionConnection()))));

        assertEquals(
                "Snapshot mode must use the domain enum.",
                WhatsAppChannelMode.BOTH,
                WhatsAppAdministrationDtoMapper.snapshot(dto)
                        .stores().get(0).mode());

        ActionResult result = WhatsAppAdministrationDtoMapper.action(
                new WhatsAppAdministrationDtos.ActionResult(
                        "set_mode",
                        31L,
                        77L,
                        "both",
                        "connected",
                        "5511999999999",
                        null,
                        null),
                true);
        assertTrue("Replay header must reach the domain.", result.replayed());
        assertEquals(
                "Action mode must use the domain enum.",
                WhatsAppChannelMode.BOTH,
                result.mode().orElseThrow());
    }

    @Test
    public void mutationRejectsQrMaterial() {
        WhatsAppAdministrationDtos.ActionResult dto =
                new WhatsAppAdministrationDtos.ActionResult(
                        "provision_evolution",
                        31L,
                        77L,
                        null,
                        "connecting",
                        null,
                        "data:image/png;base64,AAAA",
                        null);
        assertThrows(
                "Mutation mapper must reject QR credentials.",
                IllegalArgumentException.class,
                () -> WhatsAppAdministrationDtoMapper.action(dto, false));
    }

    @Test
    public void qrRequiresDedicatedActionAndEphemeralMaterial() {
        EphemeralQr qr = WhatsAppAdministrationDtoMapper.qr(
                new WhatsAppAdministrationDtos.ActionResult(
                        "request_evolution_qr",
                        31L,
                        77L,
                        null,
                        "connecting",
                        null,
                        null,
                        "PAIR-123"));
        assertEquals(
                "QR mapper must preserve the ephemeral pairing code.",
                "PAIR-123",
                qr.pairingCode().orElseThrow());

        assertThrows(
                "Only the dedicated QR action can produce QR state.",
                IllegalArgumentException.class,
                () -> WhatsAppAdministrationDtoMapper.qr(
                        new WhatsAppAdministrationDtos.ActionResult(
                                "refresh_evolution",
                                31L,
                                77L,
                                null,
                                "connected",
                                null,
                                null,
                                null)));
    }

    private static WhatsAppAdministrationDtos.Connection evolutionConnection() {
        return new WhatsAppAdministrationDtos.Connection(
                77L,
                31L,
                "evolution",
                "WhatsApp Evolution",
                "connected",
                true,
                true,
                "5511999999999",
                null,
                "store-31",
                false,
                "2026-08-01T11:00:00Z",
                "2026-08-01T11:05:00Z",
                "2026-08-01T11:05:00Z");
    }
}
