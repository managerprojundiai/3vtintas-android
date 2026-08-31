package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.util.Optional;

final class FakeSessionVault implements SessionVault {
    private Optional<PersistedSession> session = Optional.empty();
    private RuntimeException loadFailure;
    private RuntimeException saveFailure;

    @Override
    public void save(PersistedSession value) {
        if (saveFailure != null) {
            throw saveFailure;
        }
        session = Optional.of(value);
    }

    @Override
    public Optional<PersistedSession> load() {
        if (loadFailure != null) {
            throw loadFailure;
        }
        return session;
    }

    @Override
    public void clear() {
        session = Optional.empty();
    }

    void failLoad(RuntimeException failure) {
        loadFailure = failure;
    }

    void failSave(RuntimeException failure) {
        saveFailure = failure;
    }
}
