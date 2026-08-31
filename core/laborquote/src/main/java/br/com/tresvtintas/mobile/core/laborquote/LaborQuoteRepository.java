package br.com.tresvtintas.mobile.core.laborquote;

import java.io.OutputStream;
import java.util.Optional;

public interface LaborQuoteRepository {
    LaborQuotePage page(
            LaborQuoteQuery query,
            Optional<String> cursor) throws LaborQuoteException;

    LaborQuoteDetail detail(long quoteId) throws LaborQuoteException;

    LaborQuotePdfDownload downloadPdf(
            long quoteId,
            OutputStream destination) throws LaborQuoteException;

    LaborQuoteMutationResult create(
            LaborQuoteDraft draft,
            String idempotencyKey) throws LaborQuoteException;

    LaborQuoteMutationResult duplicate(
            long quoteId,
            String idempotencyKey) throws LaborQuoteException;

    LaborQuoteMutationResult update(
            long quoteId,
            int expectedRevision,
            LaborQuoteDraft draft,
            String idempotencyKey) throws LaborQuoteException;

    LaborQuoteStatusMutationResult transition(
            long quoteId,
            int expectedRevision,
            LaborQuoteStatus status,
            String idempotencyKey) throws LaborQuoteException;
}
