package br.com.tresvtintas.mobile.data.accountaccess;

import java.util.regex.Pattern;

public record ManagedAccountAccessScope(
        long actorUserId,
        long targetUserId,
        String authorizationRevision) {
    private static final Pattern REVISION =
            Pattern.compile("^[0-9a-f]{64}$");

    public ManagedAccountAccessScope {
        if (actorUserId < 1 || targetUserId < 1) {
            throw new IllegalArgumentException(
                    "Managed account access user scope is invalid.");
        }
        if (authorizationRevision == null
                || !REVISION.matcher(authorizationRevision).matches()) {
            throw new IllegalArgumentException(
                    "Managed account access authorization is invalid.");
        }
    }
}
