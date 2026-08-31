package br.com.tresvtintas.mobile.core.accountaccess;

@FunctionalInterface
public interface AccountRevocationListener {
    void onAccountAccessRevoked(AccountRevocation revocation);
}
