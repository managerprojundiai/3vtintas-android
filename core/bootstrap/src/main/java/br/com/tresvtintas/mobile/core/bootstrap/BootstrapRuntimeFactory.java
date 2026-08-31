package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class BootstrapRuntimeFactory {
    private BootstrapRuntimeFactory() {
        throw new AssertionError("No instances.");
    }

    public static BootstrapController create(
            MobileApi protectedApi,
            ClientCompatibility compatibility,
            Executor workerExecutor,
            Executor mainExecutor) {
        BootstrapRepository repository = new BootstrapRepository(
                new RetrofitBootstrapRemote(Objects.requireNonNull(
                        protectedApi, "Protected API is required.")),
                compatibility);
        return new BootstrapController(repository, workerExecutor, mainExecutor);
    }
}
