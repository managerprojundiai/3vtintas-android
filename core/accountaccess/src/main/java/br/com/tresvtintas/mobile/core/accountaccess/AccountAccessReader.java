package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Optional;

@FunctionalInterface
public interface AccountAccessReader {
    AccountAccessPage page(
            AccountAccessView view,
            Optional<String> cursor,
            int limit) throws AccountAccessException;
}
