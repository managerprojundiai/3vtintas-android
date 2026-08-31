package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.OptionalLong;
import org.junit.Test;

public final class DeliveryManagementCommandTest {
    @Test
    public void assignmentActionDistinguishesAssignAndUnassign() {
        DeliveryManagementCommand.Assignment assign =
                new DeliveryManagementCommand.Assignment(
                        7,
                        501,
                        3,
                        OptionalLong.of(21));
        DeliveryManagementCommand.Assignment unassign =
                new DeliveryManagementCommand.Assignment(
                        7,
                        501,
                        3,
                        OptionalLong.empty());

        assertEquals(
                "A present driver must map to assignment.",
                DeliveryManagementAction.ASSIGN,
                assign.action());
        assertEquals(
                "An absent driver must map to unassignment.",
                DeliveryManagementAction.UNASSIGN,
                unassign.action());
    }

    @Test
    public void rejectsInvalidRevisionAndDurationBeforeNetwork() {
        assertThrows(
                "A stale or absent revision must be rejected locally.",
                IllegalArgumentException.class,
                () -> new DeliveryManagementCommand.Completion(7, 501, 0));
        assertThrows(
                "A duration above one day must be rejected locally.",
                IllegalArgumentException.class,
                () -> new DeliveryManagementCommand.Schedule(
                        7,
                        501,
                        3,
                        Instant.EPOCH,
                        1_441));
    }
}
