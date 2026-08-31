package br.com.tresvtintas.mobile.core.accountaccess;

@FunctionalInterface
public interface AccountAccessStateListener {
    void onAccountAccessStateChanged(AccountAccessState state);
}
