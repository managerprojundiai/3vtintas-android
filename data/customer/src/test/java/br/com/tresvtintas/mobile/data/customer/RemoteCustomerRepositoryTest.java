package br.com.tresvtintas.mobile.data.customer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerMutationResult;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.data.customer.remote.CustomerRemote;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class RemoteCustomerRepositoryTest {
    private static final String REVISION_A = "a".repeat(64);
    private static final String REVISION_B = "b".repeat(64);

    @Test
    public void accountKeyChangesAcrossIdentityAuthorizationAndStore() {
        String base = scope(10, REVISION_A, 20).accountKey();

        assertNotEquals(
                "Another user must receive an isolated scope.",
                base,
                scope(11, REVISION_A, 20).accountKey());
        assertNotEquals(
                "Authorization revision change invalidates customer state.",
                base,
                scope(10, REVISION_B, 20).accountKey());
        assertNotEquals(
                "Store change invalidates customer state.",
                base,
                scope(10, REVISION_A, 21).accountKey());
    }

    @Test
    public void closeRevokesEverySubsequentCustomerRead() {
        RemoteCustomerRepository repository = new RemoteCustomerRepository(
                scope(10, REVISION_A, 20),
                new FakeRemote());
        repository.close();

        CustomerException exception = assertThrows(
                "Old Activity controllers must fail after logout or re-scope.",
                CustomerException.class,
                () -> repository.page(
                        CustomerQuery.initial(),
                        Optional.empty()));
        assertEquals(
                "Revocation is distinct from a network outage.",
                CustomerFailureKind.ACCESS_REVOKED,
                exception.kind());
    }

    @Test
    public void repositoryNeverAcceptsAClientOrganizationOutsideScope() {
        RemoteCustomerRepository repository = new RemoteCustomerRepository(
                scope(10, REVISION_A, 20),
                new FakeRemote());

        CustomerException exception = assertThrows(
                "UI cannot substitute another organization.",
                CustomerException.class,
                () -> repository.create(
                        OptionalLong.of(21),
                        draft(),
                        "00000000-0000-4000-8000-000000000071"));
        assertEquals(
                "Scope mismatch fails locally and remains server-authorized.",
                CustomerFailureKind.ACCESS_REVOKED,
                exception.kind());
    }

    private static CustomerAccountScope scope(
            long userId,
            String revision,
            long organizationId) {
        return new CustomerAccountScope(
                userId,
                revision,
                OptionalLong.of(organizationId));
    }

    private static CustomerDraft draft() {
        return CustomerDraft.fromRaw(
                "Cliente",
                "",
                "",
                "",
                "",
                "",
                "",
                "");
    }

    private static final class FakeRemote implements CustomerRemote {
        @Override
        public CustomerPage page(
                CustomerQuery query,
                Optional<String> cursor) {
            return new CustomerPage(java.util.List.of(), Optional.empty());
        }

        @Override
        public CustomerDetail detail(long customerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CustomerMutationResult create(
                OptionalLong organizationId,
                CustomerDraft customer,
                String idempotencyKey) {
            return new CustomerMutationResult(90, true, false);
        }

        @Override
        public CustomerMutationResult update(
                long customerId,
                CustomerDraft customer,
                String idempotencyKey) {
            throw new UnsupportedOperationException();
        }
    }
}
