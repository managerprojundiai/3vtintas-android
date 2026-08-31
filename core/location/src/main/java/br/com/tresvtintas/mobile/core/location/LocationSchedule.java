package br.com.tresvtintas.mobile.core.location;

import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class LocationSchedule {
    private LocationSchedule() {
        throw new AssertionError("No instances.");
    }

    public static boolean active(
            LocationDtos.Policy policy,
            Instant instant) {
        Objects.requireNonNull(policy, "Location policy is required.");
        Objects.requireNonNull(instant, "Location instant is required.");
        ZonedDateTime local = instant.atZone(ZoneId.of(policy.timeZone()));
        int dayBit = dayBit(local.getDayOfWeek());
        int minute = local.getHour() * 60 + local.getMinute();
        return policy.enabled()
                && (policy.weekdayMask() & dayBit) != 0
                && minute >= policy.startsAtMinute()
                && minute < policy.endsAtMinute();
    }

    private static int dayBit(DayOfWeek day) {
        return switch (day) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 4;
            case WEDNESDAY -> 8;
            case THURSDAY -> 16;
            case FRIDAY -> 32;
            case SATURDAY -> 64;
        };
    }
}
