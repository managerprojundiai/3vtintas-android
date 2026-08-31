package br.com.tresvtintas.mobile.core.painteradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class PainterAdministrationModelsTest {
    @Test
    public void normalizesPainterDraftBeforeTransport() {
        PainterDraft draft = new PainterDraft(
                7,
                "  Maria Silva  ",
                "  maria@example.com  ",
                Optional.of(" 123 "),
                Optional.empty(),
                Optional.empty(),
                Optional.of("  Pinturas Maria "),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(" Jundiaí "),
                Optional.of("sp"),
                new BigDecimal("4.25"),
                OptionalLong.of(19),
                Optional.of("  Atendimento residencial "));

        assertEquals(
                "Names must be normalized before leaving the feature.",
                "Maria Silva",
                draft.name());
        assertEquals(
                "Emails must be normalized before leaving the feature.",
                "maria@example.com",
                draft.email());
        assertEquals(
                "Optional PII must be normalized before transport.",
                Optional.of("123"),
                draft.cpf());
        assertEquals(
                "Federation units must use the canonical representation.",
                Optional.of("SP"),
                draft.state());
    }

    @Test
    public void rejectsInvalidCommissionAndIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> draft(new BigDecimal("100.01")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PainterDraft(
                        7,
                        "Maria",
                        "not-an-email",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        BigDecimal.ZERO,
                        OptionalLong.empty(),
                        Optional.empty()));
    }

    @Test
    public void queryNormalizesFiltersAndRejectsUnsafeBounds() {
        PainterAdministrationQuery query =
                PainterAdministrationQuery.initial()
                        .withSearch("  Maria  ")
                        .withOrganization(OptionalLong.of(8))
                        .withStatus(Optional.of(
                                PainterAdministrationStatus.ACTIVE));

        assertEquals(
                "Search text must be normalized.",
                Optional.of("Maria"),
                query.search());
        assertEquals(
                "The selected organization must be retained.",
                OptionalLong.of(8),
                query.organizationId());
        assertThrows(
                IllegalArgumentException.class,
                () -> new PainterAdministrationQuery(
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.of("x".repeat(81)),
                        30));
    }

    @Test
    public void optionCollectionsCannotBeMutatedByCallers() {
        List<Organization> organizations = new ArrayList<>();
        organizations.add(new Organization(7, "Jundiaí"));
        Options options = new Options(
                organizations,
                List.of(new Manager(
                        19,
                        "Gerente",
                        OptionalLong.of(7))));

        organizations.clear();

        assertEquals(
                "The domain must retain an immutable snapshot.",
                1,
                options.organizations().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> options.organizations().add(
                        new Organization(8, "Itatiba")));
        assertTrue(
                "Managers must be scoped by organization.",
                options.managersFor(7).stream()
                        .allMatch(manager -> manager.id() == 19));
    }

    private static PainterDraft draft(BigDecimal rate) {
        return new PainterDraft(
                7,
                "Maria",
                "maria@example.com",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                rate,
                OptionalLong.empty(),
                Optional.empty());
    }
}
