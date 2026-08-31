package br.com.tresvtintas.mobile.core.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

public final class AdaptiveLocationSamplerTest {
    private static final Instant START = Instant.parse("2026-08-13T12:00:00Z");
    private static final Duration TWO_MINUTES = Duration.ofMinutes(2L);

    @Test
    public void acceptsFirstPointAndMeaningfulMovementAtConfiguredCadence() {
        AdaptiveLocationSampler sampler = new AdaptiveLocationSampler();
        assertTrue("The first trustworthy fix anchors the route.", sampler.shouldAccept(
                sample(-23.0000D, -46.0000D, 12F, null, START), TWO_MINUTES));
        assertFalse("Movement before the configured cadence must be coalesced.",
                sampler.shouldAccept(
                        sample(-23.0004D, -46.0000D, 12F, 4F,
                                START.plusSeconds(60L)),
                        TWO_MINUTES));
        assertTrue("Meaningful movement at the configured cadence must be retained.",
                sampler.shouldAccept(
                        sample(-23.0008D, -46.0000D, 12F, 4F,
                                START.plusSeconds(120L)),
                        TWO_MINUTES));
    }

    @Test
    public void coalescesStationaryNoiseButKeepsStopEvidence() {
        AdaptiveLocationSampler sampler = new AdaptiveLocationSampler();
        assertTrue("The first fix must be accepted.", sampler.shouldAccept(
                sample(-23D, -46D, 20F, 0F, START), TWO_MINUTES));
        assertFalse("Stationary GPS noise must not create a point every two minutes.",
                sampler.shouldAccept(
                        sample(-23.00001D, -46.00001D, 20F, 0F,
                                START.plusSeconds(120L)),
                        TWO_MINUTES));
        assertTrue("A spaced stationary point is required to calculate stop duration.",
                sampler.shouldAccept(
                        sample(-23.00001D, -46.00001D, 20F, 0F,
                                START.plusSeconds(600L)),
                        TWO_MINUTES));
    }

    @Test
    public void rejectsImpreciseAndOutOfOrderFixes() {
        AdaptiveLocationSampler sampler = new AdaptiveLocationSampler();
        assertFalse("Very imprecise fixes must not distort a route.", sampler.shouldAccept(
                sample(-23D, -46D, 250F, null, START), TWO_MINUTES));
        assertTrue("A later precise fix must still anchor the route.", sampler.shouldAccept(
                sample(-23D, -46D, 15F, null, START.plusSeconds(10L)), TWO_MINUTES));
        assertFalse("Older fixes must never rewrite route order.", sampler.shouldAccept(
                sample(-23.001D, -46D, 15F, 5F, START), TWO_MINUTES));
    }

    @Test
    public void resetStartsANewScheduleWindow() {
        AdaptiveLocationSampler sampler = new AdaptiveLocationSampler();
        AdaptiveLocationSampler.Sample point = sample(-23D, -46D, 10F, null, START);
        assertTrue("Initial point must be accepted.", sampler.shouldAccept(point, TWO_MINUTES));
        sampler.reset();
        assertTrue("A new schedule window needs a fresh route anchor.",
                sampler.shouldAccept(point, TWO_MINUTES));
    }

    private static AdaptiveLocationSampler.Sample sample(
            double latitude,
            double longitude,
            float accuracy,
            Float speed,
            Instant recordedAt) {
        return new AdaptiveLocationSampler.Sample(
                latitude,
                longitude,
                accuracy,
                speed,
                recordedAt);
    }
}
