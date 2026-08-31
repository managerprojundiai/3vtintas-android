package br.com.tresvtintas.mobile.core.network.dto;

public record AttendanceReplyRequest(
        String content,
        String confirmation) {
    public AttendanceReplyRequest {
        if (content == null
                || content.isBlank()
                || content.length() > 3_500
                || !"SEND_ATTENDANCE_REPLY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Attendance reply request is invalid.");
        }
    }
}
