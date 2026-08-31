package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AccountAccessSnapshot(
        AccountAccessView view,
        List<AccountAccessEntry> items,
        Optional<String> nextCursor) {
    public AccountAccessSnapshot {
        if (view == null || items == null) {
            throw new IllegalArgumentException(
                    "Account access snapshot is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null
                ? Optional.empty()
                : nextCursor;
        for (AccountAccessEntry item : items) {
            if (!matches(view, item)) {
                throw new IllegalArgumentException(
                        "Account access entry does not match its view.");
            }
        }
    }

    public static AccountAccessSnapshot from(
            AccountAccessView view,
            AccountAccessPage page) {
        if (page == null) {
            throw new IllegalArgumentException(
                    "Account access page is required.");
        }
        return new AccountAccessSnapshot(
                view,
                page.items(),
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }

    public Optional<AccountAccessEntry> entry(String id) {
        return items.stream()
                .filter(item -> item.id().equals(id))
                .findFirst();
    }

    public AccountAccessSnapshot append(AccountAccessPage page) {
        if (page == null) {
            throw new IllegalArgumentException(
                    "Account access page is required.");
        }
        Map<String, AccountAccessEntry> combined =
                new LinkedHashMap<>();
        items.forEach(item -> combined.put(item.id(), item));
        page.items().forEach(item -> {
            if (!matches(view, item)) {
                throw new IllegalArgumentException(
                        "Account access page changed view.");
            }
            combined.put(item.id(), item);
        });
        return new AccountAccessSnapshot(
                view,
                List.copyOf(combined.values()),
                page.nextCursor());
    }

    public AccountAccessSnapshot without(String targetId) {
        return new AccountAccessSnapshot(
                view,
                items.stream()
                        .filter(item -> !item.id().equals(targetId))
                        .toList(),
                nextCursor);
    }

    private static boolean matches(
            AccountAccessView view,
            AccountAccessEntry entry) {
        return view == AccountAccessView.DEVICES
                ? entry instanceof AccountDevice
                : entry instanceof AccountSession;
    }
}
