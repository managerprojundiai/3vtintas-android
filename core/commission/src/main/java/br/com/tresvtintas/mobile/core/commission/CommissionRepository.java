package br.com.tresvtintas.mobile.core.commission;

import java.util.Optional;

public interface CommissionRepository {
    CommissionPage page(CommissionQuery query, Optional<String> cursor)
            throws CommissionException;

    CommissionDetail detail(long commissionId, CommissionScope scope)
            throws CommissionException;

    CommissionMutationResult transition(
            CommissionMutationCommand command,
            String idempotencyKey) throws CommissionException;
}
