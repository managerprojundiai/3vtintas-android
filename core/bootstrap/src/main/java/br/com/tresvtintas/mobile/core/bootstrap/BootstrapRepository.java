package br.com.tresvtintas.mobile.core.bootstrap;

import java.util.Objects;

final class BootstrapRepository {
    private final BootstrapRemote remote;
    private final BootstrapMapper mapper;

    BootstrapRepository(BootstrapRemote remote, ClientCompatibility compatibility) {
        this.remote = Objects.requireNonNull(remote, "Bootstrap remote is required.");
        this.mapper = new BootstrapMapper(compatibility);
    }

    BootstrapSnapshot load(ExpectedBootstrapIdentity expected) throws BootstrapException {
        return mapper.map(remote.load(), expected);
    }
}
