package br.com.tresvtintas.mobile.core.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class TeamSnapshotTest {
    @Test
    public void snapshotRequiresSummaryCountToMatchMembers() {
        TeamSummary summary = new TeamSummary(
                1,
                0,
                0,
                new BigDecimal("0.00"),
                Optional.empty());

        assertThrows(
                "Mismatched member totals must fail closed.",
                IllegalArgumentException.class,
                () -> new TeamSnapshot(
                        Instant.EPOCH,
                        new TeamContext(
                                TeamContext.Visibility.ALL,
                                Optional.empty()),
                        summary,
                        List.of(),
                        List.of()));
    }

    @Test
    public void stateKeepsRequestIdForSupport() {
        TeamState state = TeamState.error(new TeamException(
                TeamFailureKind.FORBIDDEN,
                "Denied.",
                "request-1",
                null));

        assertEquals(
                "Support request ID must survive presentation mapping.",
                "request-1",
                state.requestId().orElseThrow());
    }
}
