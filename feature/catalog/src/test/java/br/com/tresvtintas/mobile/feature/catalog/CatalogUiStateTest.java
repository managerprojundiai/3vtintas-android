package br.com.tresvtintas.mobile.feature.catalog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.catalog.CatalogSnapshot;
import br.com.tresvtintas.mobile.core.catalog.CatalogSource;
import br.com.tresvtintas.mobile.core.catalog.CatalogState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class CatalogUiStateTest {
    @Test
    public void keepsItemsVisibleWhileRefreshingAndLoadingMore() {
        CatalogSnapshot snapshot = snapshot();

        CatalogUiState refreshing = CatalogUiState.from(
                CatalogState.refreshing(snapshot));
        CatalogUiState loadingMore = CatalogUiState.from(
                CatalogState.loadingMore(snapshot));

        assertTrue("Refresh must preserve the current list.", refreshing.showList());
        assertTrue("Refresh indicator must be explicit.", refreshing.refreshing());
        assertTrue("Pagination must preserve the current list.", loadingMore.showList());
        assertTrue("Pagination state must disable duplicate load.", loadingMore.loadingMore());
    }

    @Test
    public void accessRevocationIsTerminalAndCannotRetryLocally() {
        CatalogUiState state = CatalogUiState.from(CatalogState.error(
                new CatalogException(
                        CatalogFailureKind.FORBIDDEN,
                        "revoked")));

        assertTrue("Revocation must show a terminal error.", state.showError());
        assertFalse("Client retry cannot recreate server capability.", state.retryAllowed());
        assertFalse("Revoked state must not expose product rows.", state.showList());
    }

    @Test
    public void emptySuccessfulResultIsNotReportedAsFailure() {
        CatalogSnapshot empty = new CatalogSnapshot(
                List.of(),
                false,
                false,
                CatalogSource.NETWORK,
                Instant.parse("2026-07-25T12:00:00Z"));

        CatalogUiState state = CatalogUiState.from(CatalogState.ready(
                empty,
                Optional.empty(),
                Optional.empty()));

        assertTrue("Empty filter result must have a dedicated state.", state.showEmpty());
        assertFalse("Empty result is not a transport failure.", state.showError());
    }

    private static CatalogSnapshot snapshot() {
        CatalogProduct product = new CatalogProduct(
                1,
                Optional.empty(),
                "Produto",
                Optional.empty(),
                Optional.empty(),
                Optional.of("SKU-1"),
                Optional.of("UN"),
                Optional.empty(),
                Optional.of(new BigDecimal("10.00")),
                Optional.of(2),
                Optional.empty(),
                Instant.parse("2026-07-25T12:00:00Z"));
        return new CatalogSnapshot(
                List.of(product),
                true,
                false,
                CatalogSource.NETWORK,
                Instant.parse("2026-07-25T12:00:00Z"));
    }
}
