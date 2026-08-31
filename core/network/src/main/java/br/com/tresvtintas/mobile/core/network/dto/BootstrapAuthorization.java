package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record BootstrapAuthorization(
        List<String> capabilities,
        OrganizationAccess organizationAccess,
        String revision) {
    private static final int MAXIMUM_CAPABILITIES = 256;

    public BootstrapAuthorization {
        if (capabilities == null || organizationAccess == null) {
            throw new IllegalArgumentException(
                    "Capabilities and organization access are required.");
        }
        capabilities = List.copyOf(capabilities);
        if (capabilities.size() > MAXIMUM_CAPABILITIES) {
            throw new IllegalArgumentException("Capability list is too large.");
        }
        for (String capability : capabilities) {
            DtoValidation.requireText(capability, "Capability", 120);
        }
        revision = DtoValidation.requireText(revision, "Authorization revision", 64);
    }
}
