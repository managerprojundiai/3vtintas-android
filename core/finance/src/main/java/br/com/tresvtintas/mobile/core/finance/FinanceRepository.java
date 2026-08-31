package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public interface FinanceRepository {
    FinancePage page(FinanceQuery query, Optional<String> cursor)
            throws FinanceException;

    FinanceDetail detail(long entryId) throws FinanceException;

    FinanceMutationResult create(FinanceDraft draft, String idempotencyKey)
            throws FinanceException;

    FinanceMutationResult settle(
            long entryId,
            FinancePaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) throws FinanceException;

    FinanceMutationResult cancel(long entryId, String idempotencyKey)
            throws FinanceException;
}
