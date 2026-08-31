package br.com.tresvtintas.mobile.feature.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationRepository;

@FunctionalInterface
public interface ManagedAccountRevocationRepositoryFactory {
    ManagedAccountRevocationRepository create(long targetUserId);
}
