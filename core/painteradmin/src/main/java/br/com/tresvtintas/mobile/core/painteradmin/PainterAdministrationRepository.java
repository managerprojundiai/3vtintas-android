package br.com.tresvtintas.mobile.core.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;

public interface PainterAdministrationRepository {
    Options options() throws PainterAdministrationException;

    Page<Painter> painters(
            PainterAdministrationQuery query,
            Optional<String> cursor) throws PainterAdministrationException;

    PainterDetail painter(long painterId)
            throws PainterAdministrationException;

    Page<AccessRequest> accessRequests(Optional<String> cursor, int pageSize)
            throws PainterAdministrationException;

    AccessRequestDetail accessRequest(long requestId)
            throws PainterAdministrationException;

    Mutation create(PainterDraft draft, String idempotencyKey)
            throws PainterAdministrationException;

    Mutation updateStatus(
            long painterId,
            int expectedRevision,
            PainterAdministrationStatus status,
            String idempotencyKey) throws PainterAdministrationException;

    Mutation updateCommission(
            long painterId,
            int expectedRevision,
            BigDecimal commissionRate,
            String idempotencyKey) throws PainterAdministrationException;

    Mutation updateManager(
            long painterId,
            int expectedRevision,
            OptionalLong managerUserId,
            String idempotencyKey) throws PainterAdministrationException;

    Mutation approvePainter(
            long requestId,
            int expectedRevision,
            long organizationId,
            OptionalLong managerUserId,
            BigDecimal commissionRate,
            String idempotencyKey) throws PainterAdministrationException;

    Mutation approveManager(
            long requestId,
            int expectedRevision,
            long organizationId,
            String idempotencyKey) throws PainterAdministrationException;

    Mutation reject(
            long requestId,
            int expectedRevision,
            String reason,
            String idempotencyKey) throws PainterAdministrationException;
}
