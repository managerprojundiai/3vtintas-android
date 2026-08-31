package br.com.tresvtintas.mobile.feature.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.audit.AgentReplayController;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.ChannelFilter;
import br.com.tresvtintas.mobile.core.audit.AgentReplayQuery;
import br.com.tresvtintas.mobile.core.audit.AgentReplayState;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
import br.com.tresvtintas.mobile.feature.audit.databinding.AgentReplayActivityBinding;
import java.util.Optional;

public final class AgentReplayActivity extends AppCompatActivity {
    private final AgentReplayController.Listener listener = this::render;
    private AgentReplayActivityBinding binding;
    private AgentReplayRenderer renderer;
    private Optional<AgentReplayController> controller = Optional.empty();
    private ChannelFilter selectedChannel = ChannelFilter.ALL;

    public static Intent intent(Context context) {
        return new Intent(context, AgentReplayActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuditPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AgentReplayActivityBinding.inflate(getLayoutInflater());
        AgentReplayAdapter adapter = new AgentReplayAdapter();
        renderer = new AgentReplayRenderer(binding, adapter);
        setContentView(binding.getRoot());
        AuditInsets.applySystemBars(binding.getRoot());
        binding.agentReplayList.setLayoutManager(new LinearLayoutManager(this));
        binding.agentReplayList.setAdapter(adapter);
        configureChannelFilter();
        binding.agentReplayToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.agentReplayApplyFilter.setOnClickListener(ignored -> applyFilter());
        binding.agentReplayRefresh.setOnClickListener(
                ignored -> controller.ifPresent(AgentReplayController::refresh));
        binding.agentReplayRetry.setOnClickListener(
                ignored -> controller.ifPresent(AgentReplayController::open));
        binding.agentReplayLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(AgentReplayController::loadMore));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AuditFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()
                || runtime.orElseThrow().agentReplayRepository().isEmpty()) {
            render(AgentReplayState.error(new AuditException(
                    AuditFailureKind.ACCESS_REVOKED,
                    "Agent replay runtime is unavailable.")));
            return;
        }
        AuditFeatureRuntime value = runtime.orElseThrow();
        AgentReplayController next = new AgentReplayController(
                value.agentReplayRepository().orElseThrow(),
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

    private void configureChannelFilter() {
        String[] labels = getResources().getStringArray(
                R.array.agent_replay_channel_labels);
        ChannelFilter[] values = ChannelFilter.values();
        if (labels.length != values.length) {
            throw new IllegalStateException("Agent replay filter labels are incomplete.");
        }
        binding.agentReplayChannel.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                labels));
        binding.agentReplayChannel.setText(labels[0], false);
        binding.agentReplayChannel.setOnItemClickListener(
                (parent, view, position, id) -> selectedChannel = values[position]);
    }

    private void applyFilter() {
        controller.ifPresent(value -> value.apply(
                AgentReplayQuery.initial().withChannel(selectedChannel)));
    }

    private void render(AgentReplayState state) {
        renderer.render(state);
    }

    private Optional<AuditFeatureRuntime> runtime() {
        return getApplication() instanceof AuditRuntimeProvider provider
                ? provider.auditRuntime()
                : Optional.empty();
    }
}
