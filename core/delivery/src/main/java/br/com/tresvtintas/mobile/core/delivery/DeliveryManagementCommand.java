package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

public sealed interface DeliveryManagementCommand
        permits DeliveryManagementCommand.Schedule,
                DeliveryManagementCommand.Assignment,
                DeliveryManagementCommand.Completion {
    long organizationId();

    long orderId();

    int expectedOrderRevision();

    DeliveryManagementAction action();

    record Schedule(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            Instant scheduledAt,
            int durationMinutes)
            implements DeliveryManagementCommand {
        public Schedule {
            validate(organizationId, orderId, expectedOrderRevision);
            Objects.requireNonNull(scheduledAt, "Schedule is required.");
            if (durationMinutes < 1 || durationMinutes > 1_440) {
                throw new IllegalArgumentException(
                        "Delivery duration is invalid.");
            }
        }

        @Override
        public DeliveryManagementAction action() {
            return DeliveryManagementAction.SCHEDULE;
        }
    }

    record Assignment(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            OptionalLong driverUserId)
            implements DeliveryManagementCommand {
        public Assignment {
            validate(organizationId, orderId, expectedOrderRevision);
            driverUserId = Objects.requireNonNull(
                    driverUserId,
                    "Driver ID is required.");
            if (driverUserId.isPresent() && driverUserId.getAsLong() < 1) {
                throw new IllegalArgumentException("Driver ID is invalid.");
            }
        }

        @Override
        public DeliveryManagementAction action() {
            return driverUserId.isPresent()
                    ? DeliveryManagementAction.ASSIGN
                    : DeliveryManagementAction.UNASSIGN;
        }
    }

    record Completion(
            long organizationId,
            long orderId,
            int expectedOrderRevision)
            implements DeliveryManagementCommand {
        public Completion {
            validate(organizationId, orderId, expectedOrderRevision);
        }

        @Override
        public DeliveryManagementAction action() {
            return DeliveryManagementAction.COMPLETE;
        }
    }

    private static void validate(
            long organizationId,
            long orderId,
            int expectedOrderRevision) {
        if (organizationId < 1 || orderId < 1 || expectedOrderRevision < 1) {
            throw new IllegalArgumentException(
                    "Delivery management command is invalid.");
        }
    }
}
