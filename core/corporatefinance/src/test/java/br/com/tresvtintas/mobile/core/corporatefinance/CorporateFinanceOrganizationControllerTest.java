package br.com.tresvtintas.mobile.core.corporatefinance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public final class CorporateFinanceOrganizationControllerTest {
    @Test
    public void normalizesSearchAndAppendsOpaqueCursorPages() {
        RecordingRepository repository = new RecordingRepository();
        CorporateFinanceOrganizationController controller =
                new CorporateFinanceOrganizationController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        AtomicReference<CorporateFinanceOrganizationState> state =
                new AtomicReference<>();
        controller.subscribe(state::set);

        controller.open("  Loja CENTRO  ");
        controller.loadMore();

        assertEquals(
                "Search must be normalized once at the domain boundary.",
                Optional.of("loja centro"),
                repository.search);
        assertEquals(
                "The second page must receive the opaque cursor.",
                Optional.of("cursor_2"),
                repository.lastCursor);
        assertEquals(
                "Both authorized organizations must remain visible.",
                2,
                state.get().snapshot().orElseThrow().items().size());
        assertEquals(
                "Pagination must finish when the server cursor ends.",
                Optional.empty(),
                state.get().snapshot().orElseThrow().nextCursor());
    }

    @Test
    public void exposesTypedFailureWithoutInventingOrganizations() {
        CorporateFinanceOrganizationRepository repository =
                (search, cursor, pageSize) -> {
                    throw new FinanceException(
                            FinanceFailureKind.FORBIDDEN,
                            "Denied.");
                };
        CorporateFinanceOrganizationController controller =
                new CorporateFinanceOrganizationController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        AtomicReference<CorporateFinanceOrganizationState> state =
                new AtomicReference<>();
        controller.subscribe(state::set);

        controller.open("");

        assertEquals(
                "Forbidden reads must end in the error phase.",
                CorporateFinanceOrganizationState.Phase.ERROR,
                state.get().phase());
        assertEquals(
                "The authorization failure must remain visible.",
                Optional.of(FinanceFailureKind.FORBIDDEN),
                state.get().failure());
        assertTrue(
                "Forbidden reads must never expose a snapshot.",
                state.get().snapshot().isEmpty());
    }

    private static final class RecordingRepository
            implements CorporateFinanceOrganizationRepository {
        private Optional<String> search = Optional.empty();
        private Optional<String> lastCursor = Optional.empty();

        @Override
        public CorporateFinanceOrganizationPage page(
                Optional<String> requestedSearch,
                Optional<String> cursor,
                int pageSize) {
            search = requestedSearch;
            lastCursor = cursor;
            if (cursor.isEmpty()) {
                return new CorporateFinanceOrganizationPage(
                        List.of(new CorporateFinanceOrganization(
                                7,
                                "Loja Centro")),
                        Optional.of("cursor_2"));
            }
            return new CorporateFinanceOrganizationPage(
                    List.of(new CorporateFinanceOrganization(
                            8,
                            "Loja Norte")),
                    Optional.empty());
        }
    }
}
