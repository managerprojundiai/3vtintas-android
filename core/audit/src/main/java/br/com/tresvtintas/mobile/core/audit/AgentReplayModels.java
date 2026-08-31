package br.com.tresvtintas.mobile.core.audit;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class AgentReplayModels {
    private AgentReplayModels() {
        throw new AssertionError("No instances.");
    }

    public static Page page(List<Turn> items, Optional<String> nextCursor) {
        return new Page(items, nextCursor);
    }

    public enum Channel {
        WHATSAPP,
        SITE_CHAT,
        MOBILE_APP,
        TELEGRAM,
        INSTAGRAM,
        MESSENGER,
        ADMIN_PANEL,
        API,
        UNKNOWN;

        public static Channel fromWireValue(String value) {
            return enumValue(Channel.class, value, "Agent replay channel");
        }
    }

    public enum Phase {
        RECEIVED,
        PLANNED,
        POLICY_CHECKED,
        ACTION_REQUESTED,
        ACTION_COMPLETED,
        RESPONSE_PREPARED,
        HUMAN_HANDOFF,
        RECOVERED,
        COMPLETED,
        OTHER;

        public static Phase fromWireValue(String value) {
            return enumValue(Phase.class, value, "Agent replay phase");
        }
    }

    public enum StepOutcome {
        OK,
        BLOCKED,
        FAILED,
        UNKNOWN;

        public static StepOutcome fromWireValue(String value) {
            return enumValue(StepOutcome.class, value, "Agent replay step outcome");
        }
    }

    public enum Outcome {
        COMPLETED,
        BLOCKED,
        FAILED,
        INCOMPLETE;

        public static Outcome fromWireValue(String value) {
            return enumValue(Outcome.class, value, "Agent replay outcome");
        }
    }

    public enum ChannelFilter {
        ALL(null),
        WHATSAPP("whatsapp"),
        SITE_CHAT("site_chat"),
        MOBILE_APP("mobile_app"),
        TELEGRAM("telegram"),
        INSTAGRAM("instagram"),
        MESSENGER("messenger"),
        ADMIN_PANEL("admin_panel"),
        API("api");

        private final String wireValue;

        ChannelFilter(String wireValue) {
            this.wireValue = wireValue;
        }

        public Optional<String> wireValue() {
            return Optional.ofNullable(wireValue);
        }
    }

    public record Step(Phase phase, StepOutcome outcome, Instant occurredAt) {
        public Step {
            Objects.requireNonNull(phase, "Agent replay phase is required.");
            Objects.requireNonNull(outcome, "Agent replay step outcome is required.");
            Objects.requireNonNull(occurredAt, "Agent replay step time is required.");
        }
    }

    public record Turn(
            Channel channel,
            Instant startedAt,
            Instant endedAt,
            Outcome outcome,
            List<Step> steps,
            boolean stepsTruncated) {
        public Turn {
            Objects.requireNonNull(channel, "Agent replay channel is required.");
            Objects.requireNonNull(startedAt, "Agent replay start is required.");
            Objects.requireNonNull(endedAt, "Agent replay end is required.");
            Objects.requireNonNull(outcome, "Agent replay outcome is required.");
            if (endedAt.isBefore(startedAt) || steps == null || steps.size() > 40
                    || steps.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Agent replay turn is invalid.");
            }
            Instant previous = startedAt;
            for (Step step : steps) {
                if (step.occurredAt().isBefore(previous)
                        || step.occurredAt().isBefore(startedAt)
                        || step.occurredAt().isAfter(endedAt)) {
                    throw new IllegalArgumentException(
                            "Agent replay steps are not chronological.");
                }
                previous = step.occurredAt();
            }
            steps = List.copyOf(steps);
        }

        @Override
        public List<Step> steps() {
            return List.copyOf(steps);
        }
    }

    public record Page(List<Turn> items, Optional<String> nextCursor) {
        public Page {
            if (items == null || items.size() > 30
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Agent replay page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = normalizeCursor(nextCursor);
        }

        @Override
        public List<Turn> items() {
            return List.copyOf(items);
        }
    }

    private static Optional<String> normalizeCursor(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String cursor = value.orElseThrow();
        if (cursor.isBlank() || cursor.length() > 160
                || !cursor.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("Agent replay cursor is invalid.");
        }
        return Optional.of(cursor);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value,
            String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(label + " is unknown.", exception);
        }
    }
}
