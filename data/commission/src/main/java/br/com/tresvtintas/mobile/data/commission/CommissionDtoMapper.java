package br.com.tresvtintas.mobile.data.commission;

import br.com.tresvtintas.mobile.core.commission.CommissionDetail;
import br.com.tresvtintas.mobile.core.commission.CommissionAction;
import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionOverview;
import br.com.tresvtintas.mobile.core.commission.CommissionPage;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionRecipientRole;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.commission.CommissionSummary;
import br.com.tresvtintas.mobile.core.network.dto.CommissionDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionOverviewDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionTotalsDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

final class CommissionDtoMapper {
    private CommissionDtoMapper() {
    }

    static CommissionPage page(CommissionPageDto value) {
        return new CommissionPage(
                value.items().stream()
                        .map(CommissionDtoMapper::summary)
                        .toList(),
                overview(value.overview()),
                Optional.ofNullable(value.nextCursor()));
    }

    static CommissionDetail detail(CommissionDetailDto value) {
        CommissionSummaryDto summaryDto = new CommissionSummaryDto(
                value.id(),
                value.kind(),
                value.status(),
                value.recipient(),
                value.organization(),
                value.order(),
                value.calculation(),
                value.revision(),
                value.eligibleForApproval(),
                value.allowedActions(),
                value.approvedAt(),
                value.paidAt(),
                value.cancelledAt(),
                value.createdAt(),
                value.updatedAt());
        return new CommissionDetail(
                summary(summaryDto),
                Optional.ofNullable(value.workflowId()),
                optionalLong(value.batchId()),
                Optional.ofNullable(value.approvedBy())
                        .map(CommissionDtoMapper::actor),
                Optional.ofNullable(value.payment())
                        .map(CommissionDtoMapper::payment),
                Optional.ofNullable(value.cancellation())
                        .map(CommissionDtoMapper::cancellation));
    }

    private static CommissionSummary summary(CommissionSummaryDto value) {
        return new CommissionSummary(
                value.id(),
                enumValue(CommissionKind.class, value.kind()),
                enumValue(CommissionStatus.class, value.status()),
                new CommissionSummary.Recipient(
                        optionalLong(value.recipient().userId()),
                        enumValue(
                                CommissionRecipientRole.class,
                                value.recipient().role()),
                        Optional.ofNullable(value.recipient().name())),
                Optional.ofNullable(value.organization())
                        .map(item -> new CommissionSummary.Organization(
                                item.id(),
                                item.name())),
                Optional.ofNullable(value.order())
                        .map(item -> new CommissionSummary.Order(
                                item.id(),
                                item.type(),
                                item.paymentStatus())),
                new CommissionSummary.Calculation(
                        value.calculation().currency(),
                        new BigDecimal(value.calculation().baseAmount()),
                        new BigDecimal(value.calculation().ratePercent()),
                        new BigDecimal(value.calculation().amount()),
                        value.calculation().ruleVersion()),
                value.revision(),
                value.eligibleForApproval(),
                value.allowedActions().stream()
                        .map(action -> enumValue(
                                CommissionAction.class,
                                action))
                        .collect(Collectors.toUnmodifiableSet()),
                optionalInstant(value.approvedAt()),
                optionalInstant(value.paidAt()),
                optionalInstant(value.cancelledAt()),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static CommissionOverview overview(CommissionOverviewDto value) {
        return new CommissionOverview(
                totals(value.pending()),
                totals(value.approved()),
                totals(value.paid()),
                totals(value.cancelled()));
    }

    private static CommissionOverview.Totals totals(CommissionTotalsDto value) {
        return new CommissionOverview.Totals(
                value.count(),
                new BigDecimal(value.amount()));
    }

    private static CommissionDetail.Actor actor(CommissionDetailDto.Actor value) {
        return new CommissionDetail.Actor(
                value.userId(),
                Optional.ofNullable(value.name()));
    }

    private static CommissionDetail.Cancellation cancellation(
            CommissionDetailDto.Cancellation value) {
        return new CommissionDetail.Cancellation(
                Optional.ofNullable(value.reason()),
                Optional.ofNullable(value.cancelledBy())
                        .map(CommissionDtoMapper::actor));
    }

    private static CommissionDetail.Payment payment(
            CommissionDetailDto.Payment value) {
        return new CommissionDetail.Payment(
                Optional.ofNullable(value.method())
                        .map(method -> enumValue(
                                CommissionPaymentMethod.class,
                                method)),
                Optional.ofNullable(value.reference()),
                Optional.ofNullable(value.paidBy())
                        .map(CommissionDtoMapper::actor));
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null
                ? Optional.empty()
                : Optional.of(Instant.parse(value));
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
