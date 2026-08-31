package br.com.tresvtintas.mobile.data.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDetail;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteLine;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePage;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePerson;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteSummary;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteLineDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteLineRequest;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuotePageDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteUpdateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

final class LaborQuoteDtoMapper {
    private LaborQuoteDtoMapper() {
    }

    static LaborQuotePage page(LaborQuotePageDto value) {
        return new LaborQuotePage(
                value.items().stream().map(LaborQuoteDtoMapper::summary).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static LaborQuoteDetail detail(LaborQuoteDetailDto value) {
        LaborQuoteSummary summary = summary(new LaborQuoteSummaryDto(
                value.id(),
                value.customer(),
                value.painter(),
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
        return new LaborQuoteDetail(
                summary,
                Optional.ofNullable(value.notes()),
                value.items().stream().map(LaborQuoteDtoMapper::line).toList());
    }

    static LaborQuoteCreateRequest createRequest(LaborQuoteDraft draft) {
        return new LaborQuoteCreateRequest(
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                draft.discount().toPlainString(),
                lines(draft));
    }

    static LaborQuoteUpdateRequest updateRequest(int revision, LaborQuoteDraft draft) {
        return new LaborQuoteUpdateRequest(
                revision,
                draft.customerId(),
                draft.title().orElse(null),
                draft.notes().orElse(null),
                draft.validUntil().map(Object::toString).orElse(null),
                draft.discount().toPlainString(),
                lines(draft));
    }

    static LaborQuoteStatusMutationRequest statusRequest(
            int revision,
            LaborQuoteStatus status) {
        return new LaborQuoteStatusMutationRequest(
                revision,
                status.name().toLowerCase(Locale.ROOT));
    }

    static LaborQuoteStatus status(String value) {
        return LaborQuoteStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private static List<LaborQuoteLineRequest> lines(LaborQuoteDraft draft) {
        return draft.items().stream()
                .map(item -> new LaborQuoteLineRequest(
                        item.description(),
                        item.quantity().toPlainString(),
                        item.unit(),
                        item.unitPrice().toPlainString()))
                .toList();
    }

    private static LaborQuoteSummary summary(LaborQuoteSummaryDto value) {
        return new LaborQuoteSummary(
                value.id(),
                new LaborQuotePerson(value.customer().id(), value.customer().name()),
                new LaborQuotePerson(value.painter().id(), value.painter().name()),
                value.organizationId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.organizationId()),
                value.title(),
                status(value.status()),
                money(value.subtotal()),
                money(value.discount()),
                money(value.total()),
                value.revision(),
                value.itemCount(),
                value.validUntil() == null
                        ? Optional.empty()
                        : Optional.of(Instant.parse(value.validUntil())),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static LaborQuoteLine line(LaborQuoteLineDto value) {
        return new LaborQuoteLine(
                value.id(),
                value.description(),
                money(value.quantity()),
                value.unit(),
                money(value.unitPrice()),
                money(value.total()));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
