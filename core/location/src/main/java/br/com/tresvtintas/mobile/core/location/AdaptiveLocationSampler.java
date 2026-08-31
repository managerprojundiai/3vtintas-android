package br.com.tresvtintas.mobile.core.location;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Decides which device fixes are valuable enough to become workforce route points.
 * Heartbeats are intentionally outside this class because they never carry coordinates.
 */
public final class AdaptiveLocationSampler {
    public record Sample(
            double latitude,
            double longitude,
            float accuracyMeters,
            Float speedMetersPerSecond,
            Instant recordedAt) {
        public Sample {
            Objects.requireNonNull(recordedAt, "Location timestamp is required.");
        }
    }

    private static final float MAXIMUM_ACCURACY_METERS = 200F;
    private static final double MINIMUM_MOVEMENT_METERS = 25D;
    private static final float MOVING_SPEED_METERS_PER_SECOND = 1.4F;
    private static final Duration STATIONARY_SAMPLE_INTERVAL = Duration.ofMinutes(10L);
    private Optional<Sample> lastAccepted = Optional.empty();

    public boolean shouldAccept(Sample candidate, Duration configuredInterval) {
        Objects.requireNonNull(candidate, "Location sample is required.");
        Objects.requireNonNull(configuredInterval, "Location interval is required.");
        if (!valid(candidate) || configuredInterval.isNegative() || configuredInterval.isZero()) {
            return false;
        }
        if (lastAccepted.isEmpty()) {
            lastAccepted = Optional.of(candidate);
            return true;
        }
        Sample previous = lastAccepted.orElseThrow();
        Duration elapsed = Duration.between(previous.recordedAt(), candidate.recordedAt());
        if (elapsed.isNegative() || elapsed.isZero()) {
            return false;
        }
        double movement = distanceMeters(previous, candidate);
        boolean moving = movement >= MINIMUM_MOVEMENT_METERS
                || speed(candidate) >= MOVING_SPEED_METERS_PER_SECOND;
        Duration required = moving ? configuredInterval : STATIONARY_SAMPLE_INTERVAL;
        if (elapsed.compareTo(required) < 0) {
            return false;
        }
        lastAccepted = Optional.of(candidate);
        return true;
    }

    public void reset() {
        lastAccepted = Optional.empty();
    }

    private static boolean valid(Sample sample) {
        return Double.isFinite(sample.latitude())
                && sample.latitude() >= -90D
                && sample.latitude() <= 90D
                && Double.isFinite(sample.longitude())
                && sample.longitude() >= -180D
                && sample.longitude() <= 180D
                && Float.isFinite(sample.accuracyMeters())
                && sample.accuracyMeters() >= 0F
                && sample.accuracyMeters() <= MAXIMUM_ACCURACY_METERS;
    }

    private static float speed(Sample sample) {
        Float speed = sample.speedMetersPerSecond();
        return speed == null || !Float.isFinite(speed) || speed < 0F ? 0F : speed;
    }

    private static double distanceMeters(Sample left, Sample right) {
        double latitudeDelta = radians(right.latitude() - left.latitude());
        double longitudeDelta = radians(right.longitude() - left.longitude());
        double leftLatitude = radians(left.latitude());
        double rightLatitude = radians(right.latitude());
        double value = Math.sin(latitudeDelta / 2D) * Math.sin(latitudeDelta / 2D)
                + Math.cos(leftLatitude) * Math.cos(rightLatitude)
                * Math.sin(longitudeDelta / 2D) * Math.sin(longitudeDelta / 2D);
        double boundedValue = Math.max(0D, Math.min(1D, value));
        return 6_371_000D * 2D * Math.atan2(
                Math.sqrt(boundedValue),
                Math.sqrt(1D - boundedValue));
    }

    private static double radians(double degrees) {
        return degrees * Math.PI / 180D;
    }
}
