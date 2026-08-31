package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;
import java.util.OptionalLong;

public interface CustomerRepository {
    CustomerPage page(
            CustomerQuery query,
            Optional<String> cursor) throws CustomerException;

    CustomerDetail detail(long customerId) throws CustomerException;

    CustomerMutationResult create(
            OptionalLong organizationId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException;

    CustomerMutationResult update(
            long customerId,
            CustomerDraft customer,
            String idempotencyKey) throws CustomerException;
}
