package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class AttendanceQueryTest {
    @Test
    public void assignedReaderStartsInMineAndKeepsServerScope() {
        AttendanceQuery query = AttendanceQuery.initial(
                OptionalLong.of(7),
                true);

        assertEquals(
                "Assigned-only readers must never request the broad default.",
                AttendanceAssignment.MINE,
                query.assignment());
        assertEquals(
                "The selected organization remains an input, not a grant.",
                7,
                query.organizationId().orElseThrow());
    }

    @Test
    public void searchIsTrimmedAndEmptyBecomesAbsent() {
        AttendanceQuery query = AttendanceQuery.initial(
                        OptionalLong.empty(),
                        false)
                .withSearch("  Cliente  ");
        AttendanceQuery empty = query.withSearch("   ");

        assertEquals(
                "Search whitespace must be normalized.",
                Optional.of("Cliente"),
                query.search());
        assertTrue(
                "Blank searches must not be sent.",
                empty.search().isEmpty());
    }

    @Test
    public void invalidInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AttendanceQuery(
                        OptionalLong.of(0),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        AttendanceAssignment.ALL,
                        30));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AttendanceQuery(
                        OptionalLong.empty(),
                        Optional.of("x".repeat(81)),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        AttendanceAssignment.ALL,
                        30));
    }
}
