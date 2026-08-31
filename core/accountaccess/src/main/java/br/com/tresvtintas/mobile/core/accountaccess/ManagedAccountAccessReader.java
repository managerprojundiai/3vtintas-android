package br.com.tresvtintas.mobile.core.accountaccess;

public abstract class ManagedAccountAccessReader
        implements AccountAccessReader, AutoCloseable {
    @Override
    public abstract void close();
}
