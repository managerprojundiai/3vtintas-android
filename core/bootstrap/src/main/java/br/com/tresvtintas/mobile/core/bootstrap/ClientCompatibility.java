package br.com.tresvtintas.mobile.core.bootstrap;

public record ClientCompatibility(
        String apiVersion,
        String contractVersion,
        int appVersionCode,
        int androidApiLevel) {

    public ClientCompatibility {
        if (apiVersion == null || apiVersion.isBlank()
                || contractVersion == null || contractVersion.isBlank()) {
            throw new IllegalArgumentException("Supported API and contract versions are required.");
        }
        if (appVersionCode < 1 || androidApiLevel < 1) {
            throw new IllegalArgumentException("Client version values must be positive.");
        }
    }
}
