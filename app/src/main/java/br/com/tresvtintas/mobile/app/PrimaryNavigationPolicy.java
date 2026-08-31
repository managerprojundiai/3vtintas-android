package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class PrimaryNavigationPolicy {
    private PrimaryNavigationPolicy() {
        throw new AssertionError("No instances.");
    }

    static List<PrimaryNavigationItem> items(Set<MobileArea> enabledAreas) {
        List<PrimaryNavigationItem> result = new ArrayList<>();
        result.add(item(
                PrimaryNavigationDestination.HOME,
                Optional.empty(),
                R.string.shell_navigation_home,
                R.drawable.shell_ic_home));
        firstEnabled(
                enabledAreas,
                MobileArea.ORDERS,
                MobileArea.MATERIAL_QUOTES,
                MobileArea.LABOR_QUOTES,
                MobileArea.CATALOG).ifPresent(area -> result.add(item(
                        PrimaryNavigationDestination.SALES,
                        Optional.of(area),
                        R.string.shell_navigation_sales,
                        R.drawable.shell_ic_sales)));
        firstEnabled(
                enabledAreas,
                MobileArea.GLOBAL_AGENDA,
                MobileArea.TEAM_AGENDA,
                MobileArea.AGENDA).ifPresent(area -> result.add(item(
                        PrimaryNavigationDestination.AGENDA,
                        Optional.of(area),
                        R.string.shell_navigation_agenda,
                        R.drawable.shell_ic_calendar)));
        firstEnabled(
                enabledAreas,
                MobileArea.CUSTOMER_SERVICE,
                MobileArea.DELIVERIES).ifPresent(area -> result.add(item(
                        PrimaryNavigationDestination.OPERATIONS,
                        Optional.of(area),
                        R.string.shell_navigation_operations,
                        R.drawable.shell_ic_operations)));
        result.add(item(
                PrimaryNavigationDestination.MORE,
                Optional.empty(),
                R.string.shell_navigation_more,
                R.drawable.shell_ic_more));
        return Collections.unmodifiableList(result);
    }

    private static Optional<MobileArea> firstEnabled(
            Set<MobileArea> enabledAreas,
            MobileArea... candidates) {
        for (MobileArea candidate : candidates) {
            if (enabledAreas.contains(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static PrimaryNavigationItem item(
            PrimaryNavigationDestination destination,
            Optional<MobileArea> target,
            int titleResource,
            int iconResource) {
        return new PrimaryNavigationItem(
                destination,
                target,
                titleResource,
                iconResource);
    }
}
