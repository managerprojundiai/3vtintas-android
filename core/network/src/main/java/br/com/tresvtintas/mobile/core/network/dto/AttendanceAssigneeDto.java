package br.com.tresvtintas.mobile.core.network.dto;

public record AttendanceAssigneeDto(long id, String displayName) {
    public AttendanceAssigneeDto {
        id = DtoValidation.requirePositive(
                id,
                "Attendance assignee ID");
        displayName = DtoValidation.requireText(
                displayName,
                "Attendance assignee name",
                200);
    }
}
