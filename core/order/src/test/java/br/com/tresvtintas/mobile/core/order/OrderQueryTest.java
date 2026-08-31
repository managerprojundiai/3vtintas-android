package br.com.tresvtintas.mobile.core.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import org.junit.Test;

public final class OrderQueryTest {
    @Test
    public void normalizesSearchAndReplacesLifecycleSafely() {
        OrderQuery query = OrderQuery.initial().withSearch("  MARIA  ").withView(OrderView.HISTORY);
        assertEquals(
                "Search must be normalized before reaching the repository.",
                Optional.of("maria"),
                query.search());
        assertEquals(
                "The selected lifecycle view must be retained.",
                OrderView.HISTORY,
                query.view());
    }

    @Test
    public void rejectsAmbiguousLifecycleAndStatus() {
        assertThrows(
                "A terminal status cannot be combined with the active lifecycle view.",
                IllegalArgumentException.class,
                () -> new OrderQuery(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(OrderStatus.DELIVERED),
                        OrderView.ACTIVE,
                        30));
    }
}
