package br.com.tresvtintas.mobile.feature.quote;

import br.com.tresvtintas.mobile.core.catalog.CatalogRepository;
import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteRepository;
import br.com.tresvtintas.mobile.core.order.OrderRepository;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

public record MaterialQuoteFeatureRuntime(
        MaterialQuoteRepository quoteRepository,
        CustomerRepository customerRepository,
        CatalogRepository catalogRepository,
        OrderRepository orderRepository,
        Executor workerExecutor,
        OptionalLong organizationId,
        boolean draftWriteAllowed,
        boolean statusWriteAllowed,
        boolean pdfReadAllowed,
        boolean orderCreateAllowed) {
    public MaterialQuoteFeatureRuntime {
        Objects.requireNonNull(quoteRepository, "Quote repository is required.");
        Objects.requireNonNull(customerRepository, "Customer repository is required.");
        Objects.requireNonNull(catalogRepository, "Catalog repository is required.");
        Objects.requireNonNull(orderRepository, "Order repository is required.");
        Objects.requireNonNull(workerExecutor, "Quote worker is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
    }
}
