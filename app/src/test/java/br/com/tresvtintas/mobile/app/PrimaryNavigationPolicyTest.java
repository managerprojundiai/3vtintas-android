package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class PrimaryNavigationPolicyTest {
    @Test
    public void completeRoleReceivesFiveStableDestinations() {
        List<PrimaryNavigationItem> items = PrimaryNavigationPolicy.items(Set.of(
                MobileArea.ORDERS,
                MobileArea.MATERIAL_QUOTES,
                MobileArea.GLOBAL_AGENDA,
                MobileArea.CUSTOMER_SERVICE,
                MobileArea.DELIVERIES));

        assertEquals(
                "Primary navigation should stay familiar across broad roles.",
                List.of(
                        PrimaryNavigationDestination.HOME,
                        PrimaryNavigationDestination.SALES,
                        PrimaryNavigationDestination.AGENDA,
                        PrimaryNavigationDestination.OPERATIONS,
                        PrimaryNavigationDestination.MORE),
                destinations(items));
        assertEquals(
                "Orders are the preferred sales landing page.",
                MobileArea.ORDERS,
                items.get(1).target().orElseThrow());
        assertEquals(
                "Service is the preferred operations landing page.",
                MobileArea.CUSTOMER_SERVICE,
                items.get(3).target().orElseThrow());
    }

    @Test
    public void restrictedRoleKeepsHomeAndMoreWithoutInventingAccess() {
        List<PrimaryNavigationItem> items = PrimaryNavigationPolicy.items(Set.of(
                MobileArea.ACCOUNT_SECURITY,
                MobileArea.NOTIFICATIONS));

        assertEquals(
                "Navigation cannot invent a business destination.",
                List.of(
                        PrimaryNavigationDestination.HOME,
                        PrimaryNavigationDestination.MORE),
                destinations(items));
    }

    private static List<PrimaryNavigationDestination> destinations(
            List<PrimaryNavigationItem> items) {
        return items.stream()
                .map(PrimaryNavigationItem::destination)
                .toList();
    }
}
