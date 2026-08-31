package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record AccountAccessPage(
        List<AccountAccessEntry> items,
        Optional<String> nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public AccountAccessPage {
        if (items == null || items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Account access page items are invalid.");
        }
        items = List.copyOf(items);
        Set<String> identities = new HashSet<>();
        for (AccountAccessEntry item : items) {
            if (item == null
                    || !identities.add(
                            item.getClass().getName() + ":" + item.id())) {
                throw new IllegalArgumentException(
                        "Account access page contains invalid duplicates.");
            }
        }
        nextCursor = nextCursor == null
                ? Optional.empty()
                : nextCursor;
        nextCursor.ifPresent(value ->
                AccountAccessValidation.text(
                        value,
                        "Account access cursor",
                        256));
    }
}
