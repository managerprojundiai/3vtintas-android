package br.com.tresvtintas.mobile.core.auth;

import android.content.Context;
import br.com.tresvtintas.mobile.core.security.EncryptedSessionStore;
import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.util.Optional;

final class EncryptedSessionVault implements SessionVault {
    private final EncryptedSessionStore store;

    EncryptedSessionVault(Context context) {
        this.store = new EncryptedSessionStore(context);
    }

    @Override
    public void save(PersistedSession session) {
        store.save(session);
    }

    @Override
    public Optional<PersistedSession> load() {
        return store.load();
    }

    @Override
    public void clear() {
        store.clear();
    }
}
