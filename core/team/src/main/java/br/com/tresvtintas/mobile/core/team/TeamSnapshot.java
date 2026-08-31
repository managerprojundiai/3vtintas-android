package br.com.tresvtintas.mobile.core.team;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TeamSnapshot(
        Instant generatedAt,
        TeamContext context,
        TeamSummary summary,
        List<TeamMember> members,
        List<TeamRegion> regions) {
    private static final int MAXIMUM_MEMBERS = 500;
    private static final int MAXIMUM_REGIONS = 5;

    public TeamSnapshot {
        Objects.requireNonNull(generatedAt, "Team generation time is required.");
        Objects.requireNonNull(context, "Team context is required.");
        Objects.requireNonNull(summary, "Team summary is required.");
        members = List.copyOf(Objects.requireNonNull(
                members,
                "Team members are required."));
        regions = List.copyOf(Objects.requireNonNull(
                regions,
                "Team regions are required."));
        if (members.size() > MAXIMUM_MEMBERS
                || regions.size() > MAXIMUM_REGIONS
                || summary.teamPainters() != members.size()) {
            throw new IllegalArgumentException("Team snapshot is invalid.");
        }
    }
}
