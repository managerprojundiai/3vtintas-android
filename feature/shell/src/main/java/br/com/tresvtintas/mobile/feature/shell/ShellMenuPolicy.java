package br.com.tresvtintas.mobile.feature.shell;

import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Intersects server authorization with journeys actually delivered by this APK. A role never grants
 * anything locally and planned areas never become clickable by accident.
 */
public final class ShellMenuPolicy {
    private ShellMenuPolicy() {
        throw new AssertionError("No instances.");
    }

    public static Set<MobileArea> enabledAreas(
            AuthorizationSnapshot authorization,
            Set<MobileArea> implementedAreas) {
        Objects.requireNonNull(authorization, "Authorization is required.");
        Objects.requireNonNull(implementedAreas, "Implemented areas are required.");
        Set<MobileArea> result = EnumSet.noneOf(MobileArea.class);
        for (MobileArea area : implementedAreas) {
            CapabilityRequirement requirement = CapabilityRequirement.of(area);
            if (requirement.satisfiedBy(authorization)) {
                result.add(area);
            }
        }
        return Set.copyOf(result);
    }

    private record CapabilityRequirement(MobileArea area) {
        static CapabilityRequirement of(MobileArea area) {
            return new CapabilityRequirement(Objects.requireNonNull(area, "Mobile area is required."));
        }

        boolean satisfiedBy(AuthorizationSnapshot authorization) {
            return area.authenticatedOnly()
                    || !area.requiredCapabilities().isEmpty()
                    && area.requiredCapabilities().stream()
                            .anyMatch(authorization::has);
        }
    }
}
