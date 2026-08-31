package br.com.tresvtintas.mobile.core.network.dto;

public record BootstrapResponse(
        AuthenticatedUser user,
        SessionIdentity session,
        BootstrapAuthorization authorization,
        BootstrapApiStatus api) {

    public BootstrapResponse {
        if (user == null || session == null || authorization == null || api == null) {
            throw new IllegalArgumentException("Bootstrap response is incomplete.");
        }
    }
}
