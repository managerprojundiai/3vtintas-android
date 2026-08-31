package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public interface LocationDtos {
    public record Policy(
            long organizationId,
            boolean enabled,
            String timeZone,
            int weekdayMask,
            int startsAtMinute,
            int endsAtMinute,
            int heartbeatSeconds,
            int locationIntervalSeconds,
            int maxBatchDelaySeconds,
            int lateAfterMinutes,
            int unavailableAfterMinutes,
            int rawRetentionDays,
            int routeRetentionDays,
            String disclosureVersion,
            long revision) {
    }

    public record Consent(
            String status,
            long policyRevision,
            String disclosureVersion,
            String acceptedAt,
            String revokedAt) {
    }

    public record DeviceState(
            String foregroundPermission,
            String backgroundPermission,
            String notificationPermission,
            boolean locationEnabled,
            String serviceState,
            Integer batteryPercent,
            String appVersion,
            String lastHeartbeatAt,
            String lastPointAt) {
    }

    public record PolicyResponse(
            Policy policy,
            boolean scheduleActive,
            Consent consent,
            DeviceState state,
            String serverTime) {
    }

    public record ConsentRequest(
            long organizationId,
            long policyRevision,
            String disclosureVersion,
            boolean accepted) {
    }

    public record ConsentResponse(
            String status,
            long policyRevision,
            String disclosureVersion,
            String changedAt) {
    }

    public record DeviceReport(
            String foregroundPermission,
            String backgroundPermission,
            String notificationPermission,
            boolean locationEnabled,
            String serviceState,
            Integer batteryPercent,
            String appVersion) {
    }

    public record HeartbeatRequest(
            long organizationId,
            DeviceReport report) {
    }

    public record HeartbeatResponse(
            String receivedAt,
            boolean scheduleActive) {
    }

    public record Point(
            int sequence,
            int latitudeE7,
            int longitudeE7,
            int accuracyMeters,
            Integer speedMillimetersPerSecond,
            Integer bearingDegrees,
            Integer altitudeCentimeters,
            Integer batteryPercent,
            boolean mocked,
            String recordedAt) {
    }

    public record BatchRequest(
            long organizationId,
            String batchKey,
            List<Point> points,
            DeviceReport report) {
        public BatchRequest {
            points = List.copyOf(points);
        }
    }

    public record BatchResponse(
            int acceptedCount,
            int rejectedCount,
            String receivedAt,
            boolean replayed) {
    }

    public record HistoryPoint(
            int latitudeE7,
            int longitudeE7,
            int accuracyMeters,
            boolean mocked,
            Integer batteryPercent,
            String recordedAt) {
    }

    public record HistoryResponse(List<HistoryPoint> items) {
        public HistoryResponse {
            items = List.copyOf(items);
        }
    }
}
