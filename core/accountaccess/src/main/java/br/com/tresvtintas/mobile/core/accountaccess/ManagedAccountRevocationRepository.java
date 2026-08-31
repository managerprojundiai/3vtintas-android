package br.com.tresvtintas.mobile.core.accountaccess;

public interface ManagedAccountRevocationRepository
        extends AutoCloseable {
    ManagedAccountRevocationPreview prepare(
            AccountAccessEntry entry) throws AccountAccessException;

    ManagedAccountRevocationChallenge challenge(
            String actionId) throws AccountAccessException;

    ManagedAccountRevocationGrant verify(
            String actionId,
            String challengeId,
            String credential) throws AccountAccessException;

    ManagedAccountRevocationResult execute(
            String actionId,
            String stepUpToken,
            String idempotencyKey) throws AccountAccessException;

    @Override
    void close();
}
