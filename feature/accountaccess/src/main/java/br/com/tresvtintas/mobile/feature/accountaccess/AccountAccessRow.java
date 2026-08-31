package br.com.tresvtintas.mobile.feature.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import java.util.Objects;

record AccountAccessRow(
        AccountAccessEntry entry,
        boolean revoking) {
    AccountAccessRow {
        Objects.requireNonNull(
                entry,
                "Account access row entry is required.");
    }
}
