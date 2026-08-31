package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class TeamResponseDtoTest {
    @Test
    public void validResponseIsAccepted() {
        TeamResponseDto.Member member = member();
        TeamResponseDto response = new TeamResponseDto(
                "2026-07-30T12:00:00Z",
                new TeamResponseDto.Context(
                        "team",
                        new TeamResponseDto.Organization(7, "Jundiaí")),
                new TeamResponseDto.Summary(
                        1,
                        2,
                        4,
                        "301.25",
                        new TeamResponseDto.Region("Jundiaí / SP", 2)),
                List.of(member),
                List.of(new TeamResponseDto.Region("Jundiaí / SP", 2)));

        assertEquals(
                "The immutable team projection must preserve the member.",
                member,
                response.members().get(0));
    }

    @Test
    public void mismatchedMemberTotalFailsClosed() {
        assertThrows(
                "A mismatched aggregate must not reach the UI.",
                IllegalArgumentException.class,
                () -> new TeamResponseDto(
                        "2026-07-30T12:00:00Z",
                        new TeamResponseDto.Context("all", null),
                        new TeamResponseDto.Summary(
                                2,
                                2,
                                4,
                                "301.25",
                                null),
                        List.of(member()),
                        List.of()));
    }

    private static TeamResponseDto.Member member() {
        return new TeamResponseDto.Member(
                9,
                "Pintor Piloto",
                "Pinturas Piloto",
                "10.00",
                "active",
                4,
                2,
                5000,
                "301.25",
                new TeamResponseDto.Region("Jundiaí / SP", 2));
    }
}
