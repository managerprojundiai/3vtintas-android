package br.com.tresvtintas.mobile.data.quote;

import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreatePreviewRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreatePreviewResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteLineDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteLineRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuotePageDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuotePricingSelectionRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuotePreviewLineDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuotePreviewPricingDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdatePreviewRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdatePreviewResponse;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteCustomer;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDetail;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSnapshot;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintSnapshot;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraft;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteLine;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePreview;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePreviewLine;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePreviewPricing;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

final class MaterialQuoteDtoMapper {
    private MaterialQuoteDtoMapper() {
    }

    static MaterialQuotePage page(MaterialQuotePageDto value) {
        return new MaterialQuotePage(
                value.items().stream().map(MaterialQuoteDtoMapper::summary).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static MaterialQuoteDetail detail(MaterialQuoteDetailDto value) {
        MaterialQuoteSummary summary = summary(new MaterialQuoteSummaryDto(
                value.id(),
                value.customer(),
                value.organizationId(),
                value.title(),
                value.status(),
                value.subtotal(),
                value.discount(),
                value.total(),
                value.revision(),
                value.itemCount(),
                value.validUntil(),
                value.createdAt(),
                value.updatedAt()));
        return new MaterialQuoteDetail(
                summary,
                Optional.ofNullable(value.notes()),
                new MaterialQuotePricingSnapshot(
                        "RESOLVED".equals(value.pricing().state()),
                        Optional.ofNullable(value.pricing().priceListCode()),
                        Optional.ofNullable(value.pricing().priceListName()),
                        Optional.ofNullable(value.pricing().priceListVersionPublicId()),
                        value.pricing().priceListVersionNumber() == null
                                ? OptionalInt.empty()
                                : OptionalInt.of(value.pricing().priceListVersionNumber()),
                        value.pricing().policyRevision() == null
                                ? OptionalInt.empty()
                                : OptionalInt.of(value.pricing().policyRevision()),
                        Optional.ofNullable(value.pricing().selectionMode()),
                        value.pricing().resolvedAt() == null
                                ? Optional.empty()
                                : Optional.of(Instant.parse(value.pricing().resolvedAt()))),
                value.items().stream().map(MaterialQuoteDtoMapper::line).toList());
    }

    static MaterialQuoteCreatePreviewRequest createPreviewRequest(
            MaterialQuoteDraft draft) {
        return new MaterialQuoteCreatePreviewRequest(
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                pricing(draft),
                requestLines(draft));
    }

    static MaterialQuoteUpdatePreviewRequest updatePreviewRequest(
            int revision,
            MaterialQuoteDraft draft) {
        return new MaterialQuoteUpdatePreviewRequest(
                revision,
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                pricing(draft),
                requestLines(draft));
    }

    static MaterialQuoteCreateRequest createRequest(
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint) {
        return new MaterialQuoteCreateRequest(
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                pricing(draft),
                requestLines(draft),
                expectedPreviewFingerprint);
    }

    static MaterialQuoteUpdateRequest updateRequest(
            int revision,
            MaterialQuoteDraft draft,
            String expectedPreviewFingerprint) {
        return new MaterialQuoteUpdateRequest(
                revision,
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                pricing(draft),
                requestLines(draft),
                expectedPreviewFingerprint);
    }

    static MaterialQuotePreview preview(MaterialQuoteCreatePreviewResponse value) {
        return preview(
                value.fingerprint(),
                value.customer().name(),
                value.subtotal(),
                "0.00",
                value.total(),
                value.pricing(),
                value.items());
    }

    static MaterialQuotePreview preview(MaterialQuoteUpdatePreviewResponse value) {
        return preview(
                value.fingerprint(),
                value.customer().name(),
                value.subtotal(),
                value.discount(),
                value.total(),
                value.pricing(),
                value.items());
    }

    private static MaterialQuotePreview preview(
            String fingerprint,
            String customerName,
            String subtotal,
            String discount,
            String total,
            MaterialQuotePreviewPricingDto pricing,
            List<MaterialQuotePreviewLineDto> items) {
        return new MaterialQuotePreview(
                fingerprint,
                customerName,
                new BigDecimal(subtotal),
                new BigDecimal(discount),
                new BigDecimal(total),
                new MaterialQuotePreviewPricing(
                        "RESOLVED".equals(pricing.state()),
                        Optional.ofNullable(pricing.priceListName()),
                        pricing.priceListVersionNumber() == null
                                ? OptionalInt.empty()
                                : OptionalInt.of(pricing.priceListVersionNumber())),
                items.stream()
                        .map(item -> new MaterialQuotePreviewLine(
                                item.productId(),
                                item.description(),
                                new BigDecimal(item.quantity()),
                                item.unit(),
                                new BigDecimal(item.unitPrice()),
                                new BigDecimal(item.total())))
                        .toList());
    }

    private static MaterialQuotePricingSelectionRequest pricing(
            MaterialQuoteDraft draft) {
        return draft.pricing()
                .map(value -> new MaterialQuotePricingSelectionRequest(
                        value.expectedPolicyRevision(),
                        value.selectedPriceListVersionPublicId().orElse(null)))
                .orElse(null);
    }

    static MaterialQuoteStatusMutationRequest statusRequest(
            int revision,
            MaterialQuoteStatus status) {
        return new MaterialQuoteStatusMutationRequest(
                revision,
                status.name().toLowerCase(Locale.ROOT));
    }

    static MaterialQuoteStatus status(String value) {
        return MaterialQuoteStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private static List<MaterialQuoteLineRequest> requestLines(
            MaterialQuoteDraft draft) {
        return draft.items().stream()
                .map(item -> new MaterialQuoteLineRequest(
                        item.productId(),
                        item.quantity().toPlainString(),
                        item.tint().map(value -> value.colorId()).orElse(null),
                        item.tint().map(value -> value.tintContextId()).orElse(null)))
                .toList();
    }

    private static MaterialQuoteSummary summary(MaterialQuoteSummaryDto value) {
        return new MaterialQuoteSummary(
                value.id(),
                new MaterialQuoteCustomer(
                        value.customer().id(),
                        value.customer().name()),
                value.organizationId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.organizationId()),
                value.title(),
                status(value.status()),
                new BigDecimal(value.subtotal()),
                new BigDecimal(value.discount()),
                new BigDecimal(value.total()),
                value.revision(),
                value.itemCount(),
                value.validUntil() == null
                        ? Optional.empty()
                        : Optional.of(Instant.parse(value.validUntil())),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static MaterialQuoteLine line(MaterialQuoteLineDto value) {
        return new MaterialQuoteLine(
                value.id(),
                value.productId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.productId()),
                value.description(),
                new BigDecimal(value.quantity()),
                Optional.ofNullable(value.unit()),
                new BigDecimal(value.unitPrice()),
                new BigDecimal(value.total()),
                value.tint() == null
                        ? Optional.empty()
                        : Optional.of(new MaterialQuoteTintSnapshot(
                                value.tint().colorId(),
                                value.tint().colorPublicId(),
                                value.tint().colorName(),
                                value.tint().tintContextId(),
                                value.tint().tintContextPublicId(),
                                value.tint().lineName(),
                                value.tint().finishName(),
                                value.tint().packageName(),
                                value.tint().packageCode(),
                                value.tint().baseCode(),
                                value.tint().unitCode())),
                value.currentProduct() == null
                        ? Optional.empty()
                        : Optional.of(value.currentProduct().active()),
                value.currentProduct() == null
                                || value.currentProduct().stock() == null
                        ? OptionalInt.empty()
                        : OptionalInt.of(value.currentProduct().stock()));
    }
}
