package br.com.tresvtintas.mobile.core.whatsappadmin;

import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import java.util.Optional;

public interface WhatsAppAdministrationRepository {
    Snapshot load() throws WhatsAppAdministrationException;

    ActionResult configureMeta(
            long organizationId,
            String phoneNumberId,
            Optional<String> phoneNumber,
            String idempotencyKey) throws WhatsAppAdministrationException;

    ActionResult provisionEvolution(
            long organizationId,
            String idempotencyKey) throws WhatsAppAdministrationException;

    ActionResult setMode(
            long organizationId,
            WhatsAppChannelMode mode,
            String idempotencyKey) throws WhatsAppAdministrationException;

    ActionResult refreshEvolution(
            long connectionId,
            String idempotencyKey) throws WhatsAppAdministrationException;

    EphemeralQr requestQr(long connectionId)
            throws WhatsAppAdministrationException;
}
