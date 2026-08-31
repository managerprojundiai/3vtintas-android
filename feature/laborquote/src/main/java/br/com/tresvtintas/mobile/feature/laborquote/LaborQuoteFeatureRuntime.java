package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record LaborQuoteFeatureRuntime(
        LaborQuoteRepository quoteRepository,
        CustomerRepository customerRepository,
        Executor workerExecutor,
        boolean draftWriteAllowed,
        boolean statusWriteAllowed,
        boolean pdfReadAllowed) {
    public LaborQuoteFeatureRuntime {
        Objects.requireNonNull(quoteRepository, "Labor quote repository is required.");
        Objects.requireNonNull(customerRepository, "Customer repository is required.");
        Objects.requireNonNull(workerExecutor, "Labor quote worker is required.");
    }
}
