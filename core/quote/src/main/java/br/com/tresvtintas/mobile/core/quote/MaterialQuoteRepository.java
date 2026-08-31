package br.com.tresvtintas.mobile.core.quote;

import java.io.OutputStream;
import java.util.Optional;
import java.util.List;

public interface MaterialQuoteRepository {
    MaterialQuotePricingContext pricingContext(long organizationId)
            throws MaterialQuoteException;

    List<MaterialQuoteTintConfiguration> tintConfigurations(
            long organizationId,
            MaterialQuotePricingSelection pricing) throws MaterialQuoteException;

    MaterialQuoteTintColorPage tintColors(
            long organizationId,
            MaterialQuotePricingSelection pricing,
            MaterialQuoteTintConfiguration configuration,
            String search,
            int limit) throws MaterialQuoteException;

    MaterialQuotePage page(
            MaterialQuoteQuery query,
            Optional<String> cursor) throws MaterialQuoteException;

    MaterialQuoteDetail detail(long quoteId) throws MaterialQuoteException;

    MaterialQuotePdfDownload downloadPdf(
            long quoteId,
            OutputStream destination) throws MaterialQuoteException;

    MaterialQuotePreview previewCreate(MaterialQuoteDraft draft)
            throws MaterialQuoteException;

    MaterialQuotePreview previewUpdate(
            long quoteId,
            int expectedRevision,
            MaterialQuoteDraft draft) throws MaterialQuoteException;

    MaterialQuoteMutationResult create(
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint,
            String idempotencyKey) throws MaterialQuoteException;

    MaterialQuoteMutationResult duplicate(
            long quoteId,
            String idempotencyKey) throws MaterialQuoteException;

    MaterialQuoteMutationResult update(
            long quoteId,
            int expectedRevision,
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint,
            String idempotencyKey) throws MaterialQuoteException;

    MaterialQuoteStatusMutationResult transition(
            long quoteId,
            int expectedRevision,
            MaterialQuoteStatus status,
            String idempotencyKey) throws MaterialQuoteException;
}
