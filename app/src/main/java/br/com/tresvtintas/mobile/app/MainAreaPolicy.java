package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellMenuPolicy;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class MainAreaPolicy {
    private MainAreaPolicy() {
        throw new AssertionError("No instances.");
    }

    static Set<MobileArea> enabledAreas(
            ShellAccessState access,
            Set<MobileArea> implementedAreas) {
        Objects.requireNonNull(access, "Shell access is required.");
        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                access.bootstrap().authorization(),
                implementedAreas);
        if (access.isOperational()) {
            return enabled;
        }
        Set<MobileArea> authenticated =
                EnumSet.noneOf(MobileArea.class);
        for (MobileArea area : enabled) {
            if (area.authenticatedOnly()) {
                authenticated.add(area);
            }
        }
        return Collections.unmodifiableSet(authenticated);
    }
}
