package br.com.tresvtintas.mobile.data.customer;

import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerMutationResult;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import br.com.tresvtintas.mobile.data.customer.remote.CustomerRemote;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Online-only customer boundary. Personal data intentionally remains outside persistent storage.
 */
public final class RemoteCustomerRepository implements CustomerRepository {
    private final CustomerAccountScope scope;
    private final CustomerRemote remote;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteCustomerRepository(
            CustomerAccountScope scope,
            CustomerRemote remote) {
        this.scope = Objects.requireNonNull(
                scope,
                "Customer account scope is required.");
        this.remote = Objects.requireNonNull(
                remote,
                "Customer remote is required.");
    }

    public CustomerAccountScope scope() {
        return scope;
    }

    @Override
    public CustomerPage page(
            CustomerQuery query,
            Optional<String> cursor) throws CustomerException {
        requireActive();
        return remote.page(query, cursor);
    }

    @Override
    public CustomerDetail detail(long customerId) throws CustomerException {
        requireActive();
        return remote.detail(customerId);
    }

    @Override
    public CustomerMutationResult create(
            OptionalLong organizationId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException {
        requireActive();
        OptionalLong effectiveOrganization = scope.organizationId();
        if (organizationId != null
                && organizationId.isPresent()
                && (effectiveOrganization.isEmpty()
                        || organizationId.getAsLong()
                                != effectiveOrganization.getAsLong())) {
            throw new CustomerException(
                    CustomerFailureKind.ACCESS_REVOKED,
                    "Customer organization scope changed.");
        }
        return remote.create(
                effectiveOrganization,
                customer,
                idempotencyKey);
    }

    @Override
    public CustomerMutationResult update(
            long customerId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException {
        requireActive();
        return remote.update(customerId, customer, idempotencyKey);
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws CustomerException {
        if (!active.get()) {
            throw new CustomerException(
                    CustomerFailureKind.ACCESS_REVOKED,
                    "Customer account scope is no longer active.");
        }
    }
}
