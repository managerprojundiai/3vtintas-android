package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class AgentActionDtoTest {
    private static final String TWO_HUNDRED = "200.00";
    private static final String PREMIUM_PAINT = "Tinta Premium";
    private static final String CUSTOMER_NAME = "Cliente 3V";
    private static final String EXPIRY = "2026-07-27T12:10:00Z";
    private static final String EXECUTED = "executed";
    private static final String STORE = "Loja Centro";

    @Test
    public void acceptsOnlyThePublishedActionContract() {
        AgentActionDto action = action("pending");
        AgentActionDecisionRequest decision =
                new AgentActionDecisionRequest(
                        "confirm",
                        "CONFIRM_AGENT_ACTION");

        assertEquals(
                "The opaque action ID must be retained.",
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                action.id());
        assertEquals(
                "Confirmation decision must remain exact.",
                "confirm",
                decision.decision());
    }

    @Test
    public void rejectsUnexpectedPresentationAndDecisionPairs() {
        assertThrows(
                "The server cannot inject a different action title.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                        "material_quote_send",
                        "pending",
                        "Clique aqui",
                        summary(),
                        null,
                        EXPIRY));
        assertThrows(
                "A reject decision cannot use the confirm literal.",
                IllegalArgumentException.class,
                () -> new AgentActionDecisionRequest(
                        "reject",
                        "CONFIRM_AGENT_ACTION"));
    }

    private static AgentActionDto action(String status) {
        return new AgentActionDto(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                "material_quote_send",
                status,
                "Enviar orçamento de material",
                summary(),
                null,
                EXPIRY);
    }

    private static AgentActionSummaryDto summary() {
        return new AgentActionSummaryDto(
                42L,
                "Tinta da fachada",
                CUSTOMER_NAME,
                null,
                null,
                null,
                null,
                null,
                "1299.90",
                "sent",
                true,
                null,
                java.util.List.of(),
                null,
                null);
    }

    @Test
    public void validatesTheCompleteCreationPreviewAndTerminalResult() {
        AgentActionItemDto item = new AgentActionItemDto(
                7,
                PREMIUM_PAINT,
                "2.00",
                "un",
                "100.00",
                TWO_HUNDRED);
        AgentActionSummaryDto summary = new AgentActionSummaryDto(
                null,
                "Pintura interna",
                CUSTOMER_NAME,
                "Parede interna",
                "2026-08-03",
                1,
                java.util.List.of(item),
                TWO_HUNDRED,
                TWO_HUNDRED,
                "draft",
                true,
                null,
                java.util.List.of(),
                null,
                null);
        AgentActionDto action = new AgentActionDto(
                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                "material_quote_create",
                EXECUTED,
                "Criar orçamento de material",
                summary,
                new AgentActionResultDto(
                        91,
                        "draft",
                        1,
                        null,
                        TWO_HUNDRED),
                EXPIRY);

        assertEquals(
                "Every reviewed line must remain in the DTO.",
                1,
                action.summary().items().size());
        assertEquals(
                "The created quote must remain navigable.",
                91,
                action.result().quoteId().longValue());
    }

    @Test
    public void acceptsServerRoundedCreationLines() {
        AgentActionItemDto item = new AgentActionItemDto(
                7,
                PREMIUM_PAINT,
                "1.25",
                "lata",
                "19.99",
                "24.99");

        assertEquals(
                "The wire preview must preserve server-side cent rounding.",
                "24.99",
                item.total());
    }

    @Test
    public void validatesTheCompleteAmendmentPreviewAndResult() {
        AgentActionItemDto previousItem = new AgentActionItemDto(
                7,
                PREMIUM_PAINT,
                "2.00",
                "un",
                "100.00",
                TWO_HUNDRED);
        AgentActionItemDto nextItem = new AgentActionItemDto(
                7,
                PREMIUM_PAINT,
                "3.00",
                "un",
                "100.00",
                "300.00");
        AgentActionSnapshotDto before = new AgentActionSnapshotDto(
                1,
                java.util.List.of(previousItem),
                TWO_HUNDRED,
                "10.00",
                "190.00");
        AgentActionSnapshotDto after = new AgentActionSnapshotDto(
                1,
                java.util.List.of(nextItem),
                "300.00",
                "10.00",
                "290.00");
        AgentActionSummaryDto summary = new AgentActionSummaryDto(
                91L,
                "Pintura interna",
                CUSTOMER_NAME,
                null,
                null,
                null,
                java.util.List.of(),
                null,
                "290.00",
                null,
                true,
                4,
                java.util.List.of(new AgentActionChangeDto(
                        "update_quantity",
                        7,
                        PREMIUM_PAINT,
                        "2.00",
                        "3.00")),
                before,
                after);
        AgentActionDto action = new AgentActionDto(
                "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                "material_quote_amend",
                EXECUTED,
                "Alterar orçamento de material",
                summary,
                new AgentActionResultDto(
                        91,
                        "draft",
                        5,
                        null,
                        "290.00"),
                EXPIRY);

        assertEquals(
                "The previous reviewed total must survive transport.",
                "190.00",
                action.summary().before().total());
        assertEquals(
                "The new quote revision must remain explicit.",
                5,
                action.result().revision().intValue());
    }

    @Test
    public void validatesTheAttendanceReplyWithoutQuoteFields() {
        AgentActionSummaryDto summary = new AgentActionSummaryDto(
                null,
                null,
                CUSTOMER_NAME,
                null,
                null,
                null,
                java.util.List.of(),
                null,
                null,
                null,
                false,
                7,
                java.util.List.of(),
                null,
                null,
                "whatsapp:701",
                "whatsapp",
                STORE,
                "Sim, entregamos hoje.",
                new AgentAttendanceLatestMessageDto(
                        "inbound",
                        "Vocês entregam hoje?",
                        "2026-07-29T12:00:00Z"),
                true);
        AgentActionDto action = new AgentActionDto(
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                "attendance_reply",
                EXECUTED,
                "Enviar resposta de atendimento",
                summary,
                new AgentActionResultDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "whatsapp:701",
                        "queued",
                        "whatsapp:701:801"),
                EXPIRY);

        assertEquals(
                "The exact reviewed response must remain available.",
                "Sim, entregamos hoje.",
                action.summary().content());
        assertEquals(
                "The terminal delivery state must be explicit.",
                "queued",
                action.result().deliveryState());
        assertThrows(
                "A quote field cannot be smuggled into an attendance action.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        action.id(),
                        action.kind(),
                        action.status(),
                        action.title(),
                        new AgentActionSummaryDto(
                                91L,
                                summary.quoteTitle(),
                                summary.customerName(),
                                summary.notes(),
                                summary.validUntil(),
                                summary.itemCount(),
                                summary.items(),
                                summary.subtotal(),
                                summary.total(),
                                summary.targetStatus(),
                                summary.pricingWillBeRevalidated(),
                                summary.expectedRevision(),
                                summary.changes(),
                                summary.before(),
                                summary.after(),
                                summary.conversationId(),
                                summary.channel(),
                                summary.organizationName(),
                                summary.content(),
                                summary.latestMessage(),
                                summary
                                        .accessAndRevisionWillBeRevalidated()),
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void validatesARevisionBoundAppointmentWithoutQuoteFields() {
        AgentAppointmentSnapshotDto before =
                new AgentAppointmentSnapshotDto(
                        "confirmed",
                        "2026-08-03T13:30:00Z",
                        60,
                        STORE);
        AgentAppointmentSnapshotDto after =
                new AgentAppointmentSnapshotDto(
                        "confirmed",
                        "2026-08-04T17:00:00Z",
                        90,
                        STORE);
        AgentAppointmentSummaryDto appointment =
                new AgentAppointmentSummaryDto(
                        71L,
                        "reschedule",
                        "Visita técnica",
                        "general",
                        "Carlos Pereira",
                        STORE,
                        CUSTOMER_NAME,
                        4,
                        before,
                        after,
                        true);
        AgentActionSummaryDto summary = new AgentActionSummaryDto(
                null,
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                null,
                null,
                null,
                false,
                null,
                java.util.List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                appointment);
        AgentActionDto action = new AgentActionDto(
                "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
                "appointment_reschedule",
                EXECUTED,
                "Reagendar compromisso",
                summary,
                new AgentActionResultDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new AgentAppointmentResultDto(
                                71,
                                "confirmed",
                                5,
                                true)),
                EXPIRY);

        assertEquals(
                "The appointment operation must survive transport.",
                "reschedule",
                action.summary().appointment().operation());
        assertThrows(
                "The action kind cannot diverge from the reviewed operation.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        action.id(),
                        "appointment_cancel",
                        action.status(),
                        "Cancelar compromisso",
                        action.summary(),
                        action.result(),
                        action.expiresAt()));
    }
}
