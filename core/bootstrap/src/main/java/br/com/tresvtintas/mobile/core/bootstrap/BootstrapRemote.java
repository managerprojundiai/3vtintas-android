package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;

@FunctionalInterface
interface BootstrapRemote {
    BootstrapResponse load() throws BootstrapException;
}
