package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.util.Optional;

interface SessionVault {
    void save(PersistedSession session);

    Optional<PersistedSession> load();

    void clear();
}
