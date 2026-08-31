package br.com.tresvtintas.mobile.data.team;

import br.com.tresvtintas.mobile.core.network.dto.TeamResponseDto;
import br.com.tresvtintas.mobile.core.team.TeamContext;
import br.com.tresvtintas.mobile.core.team.TeamMember;
import br.com.tresvtintas.mobile.core.team.TeamRegion;
import br.com.tresvtintas.mobile.core.team.TeamSnapshot;
import br.com.tresvtintas.mobile.core.team.TeamSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

final class TeamDtoMapper {
    private TeamDtoMapper() {
    }

    static TeamSnapshot snapshot(TeamResponseDto value) {
        return new TeamSnapshot(
                Instant.parse(value.generatedAt()),
                context(value.context()),
                summary(value.summary()),
                value.members().stream()
                        .map(TeamDtoMapper::member)
                        .toList(),
                value.regions().stream()
                        .map(TeamDtoMapper::region)
                        .toList());
    }

    private static TeamContext context(TeamResponseDto.Context value) {
        return new TeamContext(
                TeamContext.Visibility.valueOf(
                        value.visibility().toUpperCase(Locale.ROOT)),
                Optional.ofNullable(value.organization())
                        .map(organization -> new TeamContext.Organization(
                                organization.id(),
                                organization.name())));
    }

    private static TeamSummary summary(TeamResponseDto.Summary value) {
        return new TeamSummary(
                value.teamPainters(),
                value.totalOrders(),
                value.totalQuotes(),
                new BigDecimal(value.totalSales()),
                Optional.ofNullable(value.topRegion())
                        .map(TeamDtoMapper::region));
    }

    private static TeamMember member(TeamResponseDto.Member value) {
        return new TeamMember(
                value.painterId(),
                value.name(),
                Optional.ofNullable(value.company()),
                new BigDecimal(value.commissionRate()),
                TeamMember.Status.valueOf(
                        value.status().toUpperCase(Locale.ROOT)),
                value.totalQuotes(),
                value.totalOrders(),
                value.conversionBasisPoints(),
                new BigDecimal(value.totalSales()),
                Optional.ofNullable(value.topRegion())
                        .map(TeamDtoMapper::region));
    }

    private static TeamRegion region(TeamResponseDto.Region value) {
        return new TeamRegion(value.label(), value.count());
    }
}
