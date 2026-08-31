package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryManagementAssignmentRequest(
        long organizationId,
        int expectedOrderRevision,
        Long driverUserId,
        String confirmation) {
    public DeliveryManagementAssignmentRequest {
        organizationId = DtoValidation.requirePositive(
                organizationId,
                "Management organization ID");
        driverUserId = DtoValidation.optionalPositive(
                driverUserId,
                "Management driver ID");
        String expected = driverUserId == null
                ? "UNASSIGN_DELIVERY_DRIVER"
                : "ASSIGN_DELIVERY_DRIVER";
        if (expectedOrderRevision < 1 || !expected.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Management assignment request is invalid.");
        }
    }

    public DeliveryManagementAssignmentRequest(
            long organizationId,
            int expectedOrderRevision,
            Long driverUserId) {
        this(
                organizationId,
                expectedOrderRevision,
                driverUserId,
                driverUserId == null
                        ? "UNASSIGN_DELIVERY_DRIVER"
                        : "ASSIGN_DELIVERY_DRIVER");
    }
}
