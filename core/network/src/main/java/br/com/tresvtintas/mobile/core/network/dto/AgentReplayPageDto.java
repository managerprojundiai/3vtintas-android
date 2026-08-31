package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public record AgentReplayPageDto(List<Turn> items, String nextCursor) {
    private static final Set<String> CHANNELS = Set.of(
            "whatsapp", "site_chat", "mobile_app", "telegram",
            "instagram", "messenger", "admin_panel", "api", "unknown");
    private static final Set<String> OUTCOMES = Set.of(
            "completed", "blocked", "failed", "incomplete");
    private static final Set<String> PHASES = Set.of(
            "received", "planned", "policy_checked", "action_requested",
            "action_completed", "response_prepared", "human_handoff",
            "recovered", "completed", "other");
    private static final Set<String> STEP_OUTCOMES = Set.of(
            "ok", "blocked", "failed", "unknown");

    public AgentReplayPageDto {
        if (items == null || items.size() > 30
                || items.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("Agent replay turns are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Agent replay cursor",
                160);
        if (nextCursor != null && !nextCursor.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("Agent replay cursor is invalid.");
        }
    }

    @Override
    public List<Turn> items() {
        return List.copyOf(items);
    }

    public record Turn(
            String channel,
            String startedAt,
            String endedAt,
            String outcome,
            List<Step> steps,
            boolean stepsTruncated) {
        public Turn {
            channel = exact(channel, CHANNELS, "Agent replay channel");
            startedAt = DtoValidation.requireInstant(startedAt, "Agent replay start");
            endedAt = DtoValidation.requireInstant(endedAt, "Agent replay end");
            outcome = exact(outcome, OUTCOMES, "Agent replay outcome");
            if (steps == null || steps.size() > 40
                    || steps.stream().anyMatch(value -> value == null)) {
                throw new IllegalArgumentException("Agent replay steps are invalid.");
            }
            steps = List.copyOf(steps);
        }

        @Override
        public List<Step> steps() {
            return List.copyOf(steps);
        }
    }

    public record Step(String phase, String outcome, String occurredAt) {
        public Step {
            phase = exact(phase, PHASES, "Agent replay phase");
            outcome = exact(outcome, STEP_OUTCOMES, "Agent replay step outcome");
            occurredAt = DtoValidation.requireInstant(
                    occurredAt,
                    "Agent replay step time");
        }
    }

    private static String exact(String value, Set<String> allowed, String label) {
        String normalized = DtoValidation.requireText(value, label, 40);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(label + " is unknown.");
        }
        return normalized;
    }
}
