package br.com.tresvtintas.mobile.core.accountaccess;

public record AccountRevocation(
        AccountAccessView view,
        String targetId,
        boolean changed,
        boolean current) {
    public AccountRevocation {
        if (view == null) {
            throw new IllegalArgumentException(
                    "Account revocation view is required.");
        }
        targetId = AccountAccessValidation.uuid(
                targetId,
                "Account revocation target ID");
    }
}
