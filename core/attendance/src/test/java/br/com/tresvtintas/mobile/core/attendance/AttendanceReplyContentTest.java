package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class AttendanceReplyContentTest {
    @Test
    public void normalizesLineEndingsAndOuterWhitespace() {
        assertEquals(
                "Client and server must share canonical line endings.",
                "Primeira\n\nSegunda",
                AttendanceReplyContent.normalize(
                        " \r\nPrimeira\r\r\rSegunda \r\n"));
    }

    @Test
    public void rejectsBlankOversizedAndForbiddenControlContent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AttendanceReplyContent.normalize(" \r\n "));
        assertThrows(
                IllegalArgumentException.class,
                () -> AttendanceReplyContent.normalize(
                        "a".repeat(
                                AttendanceReplyContent.MAXIMUM_LENGTH
                                        + 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> AttendanceReplyContent.normalize(
                        "conteúdo\u0000oculto"));
    }
}
