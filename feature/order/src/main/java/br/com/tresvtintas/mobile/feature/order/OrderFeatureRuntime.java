package br.com.tresvtintas.mobile.feature.order;

import br.com.tresvtintas.mobile.core.order.OrderRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record OrderFeatureRuntime(OrderRepository repository, Executor workerExecutor) {
    public OrderFeatureRuntime {
        Objects.requireNonNull(repository, "Order repository is required.");
        Objects.requireNonNull(workerExecutor, "Order worker is required.");
    }
}
