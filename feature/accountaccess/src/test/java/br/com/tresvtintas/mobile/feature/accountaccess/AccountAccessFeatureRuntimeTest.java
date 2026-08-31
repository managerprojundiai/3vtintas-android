package br.com.tresvtintas.mobile.feature.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessRepository;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStatus;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountAccessReader;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationRepository;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationChallenge;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationGrant;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationPreview;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationResult;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.AccountRevocation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public final class AccountAccessFeatureRuntimeTest {
    private static final long MINIMUM_USER_ID = 1L;

    @Test
    public void runtimeRetainsExplicitSessionRejectionBoundary() {
        AtomicBoolean rejected = new AtomicBoolean();
        AccountAccessFeatureRuntime runtime =
                new AccountAccessFeatureRuntime(
                        repository(),
                        Runnable::run,
                        () -> rejected.set(true));

        runtime.sessionRejectionHandler().run();

        assertTrue(
                "Current-access revocation must reach the auth boundary.",
                rejected.get());
    }

    @Test
    public void runtimeRejectsMissingSecurityDependencies() {
        assertThrows(
                NullPointerException.class,
                () -> new AccountAccessFeatureRuntime(
                        null,
                        Runnable::run,
                        () -> {
                        }));
        assertThrows(
                NullPointerException.class,
                () -> new AccountAccessFeatureRuntime(
                        repository(),
                        null,
                        () -> {
                        }));
    }

    @Test
    public void textClassifiesRetryAndOpaqueIdentifiers() {
        assertEquals(
                "Only a short non-authoritative identifier is displayed.",
                "10000000",
                AccountAccessText.shortId(
                        "10000000-0000-4000-8000-000000000001"));
        assertTrue(
                "Network interruption must offer an explicit retry.",
                AccountAccessText.retryable(
                        AccountAccessFailureKind.NETWORK));
        assertFalse(
                "Rejected authentication must not loop through retries.",
                AccountAccessText.retryable(
                        AccountAccessFailureKind.AUTH_REJECTED));
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountAccessText.shortId("short"));
    }

    @Test
    public void rowTracksOnlyTheVisibleMutation() {
        AccountDevice device = new AccountDevice(
                "10000000-0000-4000-8000-000000000001",
                "Tablet",
                Optional.empty(),
                Optional.empty(),
                35,
                "0.30.0",
                AccountAccessStatus.ACTIVE,
                Instant.parse("2026-07-27T08:00:00Z"),
                Instant.parse("2026-07-27T09:00:00Z"),
                Optional.empty(),
                true);

        AccountAccessRow row = new AccountAccessRow(
                device,
                true);

        assertEquals(
                "The immutable row must retain the server entry.",
                device,
                row.entry());
        assertTrue(
                "Only the mutating button is disabled by presentation state.",
                row.revoking());
    }

    @Test
    public void managedRuntimeOwnsReaderAndProtectedRevocationSource() {
        AtomicBoolean closed = new AtomicBoolean();
        ManagedAccountAccessFeatureRuntime runtime =
                new ManagedAccountAccessFeatureRuntime(
                        userId -> managedReader(userId, closed),
                        userId -> revocationRepository(),
                        Runnable::run,
                        (activity, clientId, nonce, callback) -> {
                        },
                        () -> {
                        });

        ManagedAccountAccessReader reader =
                runtime.readerFactory().create(42);
        ManagedAccountRevocationRepository revocation =
                runtime.revocationFactory().create(42);
        reader.close();
        revocation.close();

        assertTrue(
                "Managed screen ownership must close its target reader.",
                closed.get());
    }

    private static AccountAccessRepository repository() {
        return new AccountAccessRepository() {
            @Override
            public AccountAccessPage page(
                    AccountAccessView view,
                    Optional<String> cursor,
                    int limit) {
                return new AccountAccessPage(
                        List.of(),
                        Optional.empty());
            }

            @Override
            public AccountRevocation revoke(
                    AccountAccessView view,
                    String targetId) {
                return new AccountRevocation(
                        view,
                        targetId,
                        true,
                        false);
            }
        };
    }

    private static ManagedAccountAccessReader managedReader(
            long userId,
            AtomicBoolean closed) {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException("Target is invalid.");
        }
        return new ManagedAccountAccessReader() {
            @Override
            public AccountAccessPage page(
                    AccountAccessView view,
                    Optional<String> cursor,
                    int limit) {
                return new AccountAccessPage(
                        List.of(),
                        Optional.empty());
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
    }

    private static ManagedAccountRevocationRepository
            revocationRepository() {
        return new ManagedAccountRevocationRepository() {
            @Override
            public ManagedAccountRevocationPreview prepare(
                    AccountAccessEntry entry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ManagedAccountRevocationChallenge challenge(
                            String actionId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ManagedAccountRevocationGrant verify(
                            String actionId,
                            String challengeId,
                            String credential) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ManagedAccountRevocationResult execute(
                            String actionId,
                            String stepUpToken,
                            String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
            }
        };
    }
}
