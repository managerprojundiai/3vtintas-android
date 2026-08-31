package br.com.tresvtintas.mobile.core.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import java.time.Instant;
import org.junit.Test;

public final class LocationScheduleTest {
    private static final LocationDtos.Policy POLICY = new LocationDtos.Policy(
            1L, true, "America/Sao_Paulo", 62, 480, 1080,
            300, 120, 300, 7, 20, 30, 90, "2026-08-02-v1", 1L);

    @Test
    public void acceptsWeekdayInsideCommercialSchedule() {
        assertTrue("Monday at noon must be tracked.", LocationSchedule.active(
                POLICY,
                Instant.parse("2026-08-03T15:00:00Z")));
    }

    @Test
    public void rejectsSundayAndAfterHours() {
        assertFalse("Sunday must remain private.", LocationSchedule.active(
                POLICY,
                Instant.parse("2026-08-02T15:00:00Z")));
        assertFalse("Weekday after hours must remain private.", LocationSchedule.active(
                POLICY,
                Instant.parse("2026-08-03T23:30:00Z")));
    }
}
