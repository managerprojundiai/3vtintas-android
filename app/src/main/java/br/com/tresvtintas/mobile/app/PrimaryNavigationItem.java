package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.Optional;

record PrimaryNavigationItem(
        PrimaryNavigationDestination destination,
        Optional<MobileArea> target,
        int titleResource,
        int iconResource) {

    PrimaryNavigationItem {
        if (destination == null || target == null) {
            throw new IllegalArgumentException(
                    "Navigation destination and target are required.");
        }
        boolean targetRequired = destination != PrimaryNavigationDestination.HOME
                && destination != PrimaryNavigationDestination.MORE;
        if (targetRequired != target.isPresent()) {
            throw new IllegalArgumentException(
                    "Navigation target does not match its destination.");
        }
        if (titleResource == 0 || iconResource == 0) {
            throw new IllegalArgumentException(
                    "Navigation resources must be valid.");
        }
    }
}
