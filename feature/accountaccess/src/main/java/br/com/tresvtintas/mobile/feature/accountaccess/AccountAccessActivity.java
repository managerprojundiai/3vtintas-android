package br.com.tresvtintas.mobile.feature.accountaccess;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessController;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessState;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStateListener;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountAccessReader;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationRepository;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationResult;
import br.com.tresvtintas.mobile.core.accountaccess.AccountRevocation;
import br.com.tresvtintas.mobile.feature.accountaccess.databinding.AccountAccessActivityBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AccountAccessActivity extends AppCompatActivity {
    private static final String EXTRA_MANAGED_USER_ID =
            "br.com.tresvtintas.mobile.accountaccess.MANAGED_USER_ID";
    private static final String EXTRA_MANAGED_USER_NAME =
            "br.com.tresvtintas.mobile.accountaccess.MANAGED_USER_NAME";
    private final AccountAccessStateListener listener = this::render;
    private final AtomicBoolean sessionRejectionHandled =
            new AtomicBoolean();
    private AccountAccessActivityBinding binding;
    private AccountAccessRenderer renderer;
    private AccountAccessAdapter adapter;
    private Optional<AccountAccessController> controller =
            Optional.empty();
    private Optional<AccountAccessFeatureRuntime> runtime =
            Optional.empty();
    private Optional<ManagedAccountAccessReader> managedReader =
            Optional.empty();
    private Optional<ManagedAccountRevocationRepository>
            managedRevocationRepository = Optional.empty();
    private Optional<ManagedAccountRevocationCoordinator>
            managedRevocationCoordinator = Optional.empty();
    private Optional<Runnable> sessionRejectionHandler =
            Optional.empty();
    private long managedUserId;

    public static Intent intent(Context context) {
        return new Intent(context, AccountAccessActivity.class);
    }

    public static Intent managedIntent(
            Context context,
            long userId,
            String userName) {
        if (userId < 1 || userName == null || userName.isBlank()) {
            throw new IllegalArgumentException(
                    "Managed account access target is invalid.");
        }
        return new Intent(context, AccountAccessActivity.class)
                .putExtra(EXTRA_MANAGED_USER_ID, userId)
                .putExtra(EXTRA_MANAGED_USER_NAME, userName);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AccountAccessPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AccountAccessActivityBinding.inflate(
                getLayoutInflater());
        managedUserId = getIntent().getLongExtra(
                EXTRA_MANAGED_USER_ID,
                0);
        boolean managed = managedUserId > 0;
        adapter = new AccountAccessAdapter(
                this::confirmRevocation);
        renderer = new AccountAccessRenderer(binding, adapter);
        setContentView(binding.getRoot());
        AccountAccessInsets.applySystemBars(binding.getRoot());
        binding.accountAccessList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.accountAccessList.setAdapter(adapter);
        binding.accountAccessToolbar.setNavigationOnClickListener(
                ignored -> finish());
        if (managed) {
            binding.accountAccessToolbar.setTitle(
                    R.string.account_access_managed_title);
            binding.accountAccessToolbar.setSubtitle(getString(
                    R.string.account_access_managed_subtitle,
                    getIntent().getStringExtra(
                            EXTRA_MANAGED_USER_NAME)));
            binding.accountAccessIntro.setText(
                    R.string.account_access_managed_intro);
        }
        binding.accountAccessRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        AccountAccessController::refresh));
        binding.accountAccessRetry.setOnClickListener(
                ignored -> controller.ifPresent(value ->
                        value.open(value.currentView())));
        binding.accountAccessLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        AccountAccessController::loadMore));
        binding.accountAccessViews
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (isChecked) {
                                switchView(checkedId);
                            }
                        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionRejectionHandled.set(false);
        if (managedUserId > 0) {
            startManaged();
            return;
        }
        runtime = runtime();
        if (runtime.isEmpty()) {
            render(AccountAccessState.error(
                    AccountAccessView.DEVICES,
                    new AccountAccessException(
                            AccountAccessFailureKind.ACCESS_REVOKED,
                            "Account access runtime is unavailable.")));
            return;
        }
        AccountAccessFeatureRuntime value = runtime.orElseThrow();
        sessionRejectionHandler = Optional.of(
                value.sessionRejectionHandler());
        AccountAccessController next = new AccountAccessController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this),
                this::revoked);
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(AccountAccessView.DEVICES);
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        managedReader.ifPresent(ManagedAccountAccessReader::close);
        managedReader = Optional.empty();
        managedRevocationCoordinator.ifPresent(
                ManagedAccountRevocationCoordinator::close);
        managedRevocationCoordinator = Optional.empty();
        managedRevocationRepository.ifPresent(
                ManagedAccountRevocationRepository::close);
        managedRevocationRepository = Optional.empty();
        runtime = Optional.empty();
        sessionRejectionHandler = Optional.empty();
        super.onStop();
    }

    private void startManaged() {
        Optional<ManagedAccountAccessFeatureRuntime> managedRuntime =
                managedRuntime();
        if (managedRuntime.isEmpty()) {
            render(AccountAccessState.error(
                    AccountAccessView.DEVICES,
                    new AccountAccessException(
                            AccountAccessFailureKind.FORBIDDEN,
                            "Managed account access runtime is unavailable.")));
            return;
        }
        ManagedAccountAccessFeatureRuntime value =
                managedRuntime.orElseThrow();
        ManagedAccountAccessReader reader =
                value.readerFactory().create(managedUserId);
        ManagedAccountRevocationRepository revocation =
                value.revocationFactory().create(managedUserId);
        managedReader = Optional.of(reader);
        managedRevocationRepository = Optional.of(revocation);
        sessionRejectionHandler = Optional.of(
                value.sessionRejectionHandler());
        AccountAccessController next =
                AccountAccessController.readOnly(
                        reader,
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        managedRevocationCoordinator = Optional.of(
                new ManagedAccountRevocationCoordinator(
                        this,
                        revocation,
                        value.googleIdTokenRequester(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this),
                        new ManagedRevocationListener()));
        next.subscribe(listener);
        next.open(AccountAccessView.DEVICES);
    }

    private void switchView(int checkedId) {
        AccountAccessView view =
                checkedId == R.id.account_access_sessions
                        ? AccountAccessView.SESSIONS
                        : AccountAccessView.DEVICES;
        controller.filter(value -> value.currentView() != view)
                .ifPresent(value -> value.open(view));
    }

    private void confirmRevocation(AccountAccessEntry entry) {
        if (managedUserId > 0) {
            managedRevocationCoordinator.ifPresent(value ->
                    value.review(entry));
            return;
        }
        int title = entry instanceof AccountDevice
                ? R.string.account_access_confirm_device_title
                : R.string.account_access_confirm_session_title;
        int message = entry.current()
                ? R.string.account_access_confirm_current_message
                : R.string.account_access_confirm_other_message;
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(
                        R.string.account_access_cancel_action,
                        null)
                .setPositiveButton(
                        R.string.account_access_confirm_action,
                        (dialog, which) -> controller.ifPresent(
                                value -> value.revoke(entry.id())))
                .show();
    }

    private void revoked(AccountRevocation result) {
        if (result.current()) {
            rejectSession();
            return;
        }
        int message = !result.changed()
                ? R.string.account_access_already_revoked
                : result.view() == AccountAccessView.DEVICES
                        ? R.string.account_access_revoked_device
                        : R.string.account_access_revoked_session;
        Snackbar.make(
                        binding.getRoot(),
                        message,
                        Snackbar.LENGTH_LONG)
                .show();
    }

    private void managedRevoked(
            ManagedAccountRevocationResult result) {
        if (result.targetUserId() != managedUserId) {
            managedFailure(
                    new AccountAccessException(
                            AccountAccessFailureKind.PROTOCOL,
                            "Managed revocation target changed."),
                    false);
            return;
        }
        Snackbar.make(
                        binding.getRoot(),
                        R.string.account_access_managed_success,
                        Snackbar.LENGTH_LONG)
                .show();
        controller.ifPresent(AccountAccessController::refresh);
    }

    private void managedFailure(
            AccountAccessException failure,
            boolean retryAvailable) {
        if (failure.kind() == AccountAccessFailureKind.AUTH_REJECTED) {
            rejectSession();
            return;
        }
        String message = getString(
                AccountAccessText.failure(failure.kind()));
        if (failure.requestId().isPresent()) {
            message = message
                    + "\n"
                    + getString(
                            R.string.account_access_request_id,
                            failure.requestId().orElseThrow());
        }
        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG);
        if (retryAvailable) {
            snackbar.setAction(
                    R.string.account_access_managed_retry,
                    ignored -> managedRevocationCoordinator.ifPresent(
                            ManagedAccountRevocationCoordinator
                                    ::retryExecution));
        }
        snackbar.show();
        if (failure.kind() == AccountAccessFailureKind.CONFLICT
                || failure.kind() == AccountAccessFailureKind.NOT_FOUND) {
            controller.ifPresent(AccountAccessController::refresh);
        }
    }

    private void render(AccountAccessState state) {
        renderer.render(state);
        if (state.failure()
                .filter(value ->
                        value == AccountAccessFailureKind.AUTH_REJECTED)
                .isPresent()) {
            rejectSession();
        } else if (state.failure()
                .filter(value ->
                        value == AccountAccessFailureKind.ACCESS_REVOKED)
                .isPresent()) {
            finish();
        }
    }

    private void rejectSession() {
        if (!sessionRejectionHandled.compareAndSet(false, true)) {
            return;
        }
        sessionRejectionHandler.ifPresent(Runnable::run);
        finish();
    }

    private Optional<AccountAccessFeatureRuntime> runtime() {
        return getApplication()
                        instanceof AccountAccessRuntimeProvider provider
                ? provider.accountAccessRuntime()
                : Optional.empty();
    }

    private Optional<ManagedAccountAccessFeatureRuntime>
            managedRuntime() {
        return getApplication()
                        instanceof ManagedAccountAccessRuntimeProvider provider
                ? provider.managedAccountAccessRuntime()
                : Optional.empty();
    }

    private final class ManagedRevocationListener
            implements ManagedAccountRevocationCoordinator.Listener {
        @Override
        public void onBusy(Optional<String> resourceId) {
            adapter.setBusyEntryId(resourceId);
        }

        @Override
        public void onSuccess(ManagedAccountRevocationResult result) {
            managedRevoked(result);
        }

        @Override
        public void onFailure(
                AccountAccessException failure,
                boolean executionRetryAvailable) {
            managedFailure(failure, executionRetryAvailable);
        }

        @Override
        public void onCanceled() {
            Snackbar.make(
                            binding.getRoot(),
                            R.string.account_access_managed_canceled,
                            Snackbar.LENGTH_SHORT)
                    .show();
        }
    }
}
