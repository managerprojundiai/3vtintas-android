package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationController;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Store;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationState;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationStateListener;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import br.com.tresvtintas.mobile.feature.whatsappadmin.databinding.WhatsAppAdminActivityBinding;
import java.util.Optional;
import java.util.concurrent.Executor;

public final class WhatsAppAdministrationActivity extends AppCompatActivity
        implements WhatsAppStoreDialog.Listener {
    private final WhatsAppAdministrationStateListener stateListener = this::render;
    private WhatsAppAdminActivityBinding binding;
    private WhatsAppStoreAdapter adapter;
    private WhatsAppStoreDialog storeDialog;
    private WhatsAppQrDialog qrDialog;
    private WhatsAppAdministrationController controller;
    private boolean subscribed;

    public static Intent intent(Context context) {
        return new Intent(context, WhatsAppAdministrationActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WhatsAppAdministrationPrivacy.protect(this);
        binding = WhatsAppAdminActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureSystemBarInsets();

        WhatsAppAdministrationFeatureRuntime runtime = runtimeProvider()
                .whatsAppAdministrationRuntime()
                .orElse(null);
        if (runtime == null) {
            finish();
            return;
        }
        Executor main = ContextCompat.getMainExecutor(this);
        controller = new WhatsAppAdministrationController(
                runtime.repository(),
                runtime.workerExecutor(),
                main);
        storeDialog = new WhatsAppStoreDialog(this, this);
        qrDialog = new WhatsAppQrDialog(
                this,
                runtime.workerExecutor(),
                main);
        configureViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (controller != null && !subscribed) {
            subscribed = true;
            controller.subscribe(stateListener);
            controller.load();
        }
    }

    @Override
    protected void onStop() {
        if (controller != null) {
            if (subscribed) {
                controller.unsubscribe(stateListener);
                subscribed = false;
            }
            controller.pause();
        }
        if (qrDialog != null) {
            qrDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            if (subscribed) {
                controller.unsubscribe(stateListener);
                subscribed = false;
            }
            controller.close();
        }
        if (storeDialog != null) {
            storeDialog.dismiss();
        }
        if (qrDialog != null) {
            qrDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void configureMeta(
            long storeId,
            String phoneNumberId,
            Optional<String> phoneNumber) {
        storeDialog.dismiss();
        controller.configureMeta(storeId, phoneNumberId, phoneNumber);
    }

    @Override
    public void provisionEvolution(long storeId) {
        storeDialog.dismiss();
        controller.provisionEvolution(storeId);
    }

    @Override
    public void setMode(long storeId, WhatsAppChannelMode mode) {
        storeDialog.dismiss();
        controller.setMode(storeId, mode);
    }

    @Override
    public void refreshEvolution(long connectionId) {
        storeDialog.dismiss();
        controller.refreshEvolution(connectionId);
    }

    @Override
    public void requestQr(long connectionId) {
        controller.requestQr(connectionId);
    }

    private WhatsAppAdministrationRuntimeProvider runtimeProvider() {
        if (getApplication() instanceof WhatsAppAdministrationRuntimeProvider provider) {
            return provider;
        }
        throw new IllegalStateException(
                "WhatsApp administration runtime is unavailable.");
    }

    private void configureViews() {
        adapter = new WhatsAppStoreAdapter(this::showStore);
        binding.stores.setLayoutManager(new LinearLayoutManager(this));
        binding.stores.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(ignored -> finish());
        binding.refresh.setOnClickListener(ignored -> controller.load());
    }

    private void configureSystemBarInsets() {
        int initialLeft = binding.getRoot().getPaddingLeft();
        int initialTop = binding.getRoot().getPaddingTop();
        int initialRight = binding.getRoot().getPaddingRight();
        int initialBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    initialLeft + safeInsets.left,
                    initialTop + safeInsets.top,
                    initialRight + safeInsets.right,
                    initialBottom + safeInsets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private void showStore(Store store) {
        storeDialog.show(store);
    }

    private void render(WhatsAppAdministrationState state) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        state.snapshot().ifPresent(snapshot -> {
            adapter.replace(snapshot.stores());
            binding.empty.setVisibility(
                    snapshot.stores().isEmpty() ? View.VISIBLE : View.GONE);
        });
        boolean busy = state.phase() == WhatsAppAdministrationState.Phase.LOADING
                || state.phase() == WhatsAppAdministrationState.Phase.MUTATING
                || state.phase() == WhatsAppAdministrationState.Phase.QR_LOADING;
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.refresh.setEnabled(!busy);
        state.failure().ifPresent(failure -> {
            Toast.makeText(
                    this,
                    WhatsAppAdministrationText.failure(this, failure.kind()),
                    Toast.LENGTH_LONG).show();
            controller.acknowledgeTransient();
        });
        state.lastAction().ifPresent(action -> {
            Toast.makeText(
                    this,
                    action.replayed()
                            ? R.string.whatsapp_admin_action_replayed
                            : R.string.whatsapp_admin_action_saved,
                    Toast.LENGTH_SHORT).show();
            controller.acknowledgeTransient();
        });
        state.ephemeralQr().ifPresent(qr -> {
            qrDialog.show(qr);
            controller.acknowledgeTransient();
        });
    }
}
