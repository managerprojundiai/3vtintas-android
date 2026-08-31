package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.Objects;

record ShellAction(
        MobileArea area,
        ShellActionSection section,
        int titleResource,
        int descriptionResource,
        int iconResource) {

    ShellAction {
        Objects.requireNonNull(area, "Mobile area is required.");
        Objects.requireNonNull(section, "Shell action section is required.");
        if (titleResource == 0 || descriptionResource == 0 || iconResource == 0) {
            throw new IllegalArgumentException(
                    "Shell action resources must be valid.");
        }
    }
}
