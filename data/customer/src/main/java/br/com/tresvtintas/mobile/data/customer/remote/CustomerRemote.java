package br.com.tresvtintas.mobile.data.customer.remote;

import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerMutationResult;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import java.util.Optional;
import java.util.OptionalLong;

public interface CustomerRemote {
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
