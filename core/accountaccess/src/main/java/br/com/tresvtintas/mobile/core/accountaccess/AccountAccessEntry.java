package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;

public sealed interface AccountAccessEntry
        permits AccountDevice, AccountSession {
    String id();

    AccountAccessStatus status();

    Instant lastSeenAt();

    boolean current();

    int revision();
}
