package br.com.tresvtintas.mobile.core.team;

public record TeamRegion(String label, int count) {
    public TeamRegion {
        if (label == null || label.isBlank() || count < 0) {
            throw new IllegalArgumentException("Team region is invalid.");
        }
    }
}
