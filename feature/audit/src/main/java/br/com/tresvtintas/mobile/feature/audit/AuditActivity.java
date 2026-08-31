package br.com.tresvtintas.mobile.feature.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.audit.AuditController;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
import br.com.tresvtintas.mobile.core.audit.AuditQuery;
import br.com.tresvtintas.mobile.core.audit.AuditState;
import br.com.tresvtintas.mobile.feature.audit.databinding.AuditActivityBinding;
import java.util.Optional;

public final class AuditActivity extends AppCompatActivity {
    private final AuditController.Listener listener = this::render;
    private AuditActivityBinding binding;
    private AuditRenderer renderer;
    private Optional<AuditController> controller = Optional.empty();

    public static Intent intent(Context context) {
        return new Intent(context, AuditActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuditPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AuditActivityBinding.inflate(getLayoutInflater());
        AuditEventAdapter adapter = new AuditEventAdapter();
        renderer = new AuditRenderer(binding, adapter);
        setContentView(binding.getRoot());
        AuditInsets.applySystemBars(binding.getRoot());
        binding.auditList.setLayoutManager(new LinearLayoutManager(this));
        binding.auditList.setAdapter(adapter);
        binding.auditToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.auditRefresh.setOnClickListener(
                ignored -> controller.ifPresent(AuditController::refresh));
        binding.auditRetry.setOnClickListener(
                ignored -> controller.ifPresent(AuditController::open));
        binding.auditLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(AuditController::loadMore));
        binding.auditApplyFilters.setOnClickListener(ignored -> applyFilters());
        binding.auditClearFilters.setOnClickListener(ignored -> clearFilters());
        binding.auditOpenAgentReplay.setOnClickListener(
                ignored -> startActivity(AgentReplayActivity.intent(this)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AuditFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(AuditState.error(new AuditException(
                    AuditFailureKind.ACCESS_REVOKED,
                    "Audit runtime is unavailable.")));
            return;
        }
        AuditFeatureRuntime value = runtime.orElseThrow();
        binding.auditOpenAgentReplay.setVisibility(
                value.agentReplayRepository().isPresent() ? View.VISIBLE : View.GONE);
        AuditController next = new AuditController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        super.onStop();
    }

    private void render(AuditState state) {
        renderer.render(state);
    }

    private void applyFilters() {
        String action = text(binding.auditActionFilter.getText());
        String entity = text(binding.auditEntityFilter.getText());
        controller.ifPresent(value -> value.apply(
                AuditQuery.initial().withAction(action).withEntity(entity)));
    }

    private void clearFilters() {
        binding.auditActionFilter.setText("");
        binding.auditEntityFilter.setText("");
        controller.ifPresent(value -> value.apply(AuditQuery.initial()));
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private Optional<AuditFeatureRuntime> runtime() {
        return getApplication() instanceof AuditRuntimeProvider provider
                ? provider.auditRuntime()
                : Optional.empty();
    }
}
