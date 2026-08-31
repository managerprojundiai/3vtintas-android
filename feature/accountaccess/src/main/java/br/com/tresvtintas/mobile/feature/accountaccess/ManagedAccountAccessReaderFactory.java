package br.com.tresvtintas.mobile.feature.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountAccessReader;

@FunctionalInterface
public interface ManagedAccountAccessReaderFactory {
    ManagedAccountAccessReader create(long targetUserId);
}
