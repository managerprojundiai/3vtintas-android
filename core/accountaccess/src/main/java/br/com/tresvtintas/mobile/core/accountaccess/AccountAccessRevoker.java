package br.com.tresvtintas.mobile.core.accountaccess;

@FunctionalInterface
public interface AccountAccessRevoker {
    AccountRevocation revoke(
            AccountAccessView view,
            String targetId) throws AccountAccessException;
}
