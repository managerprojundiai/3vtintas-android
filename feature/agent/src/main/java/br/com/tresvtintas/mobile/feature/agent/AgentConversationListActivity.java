package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.agent.AgentConversationListController;
import br.com.tresvtintas.mobile.core.agent.AgentConversationListState;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentActivityConversationListBinding;
import java.util.Optional;

public final class AgentConversationListActivity
        extends AppCompatActivity {
    private final AgentConversationListController.Listener listener =
            this::render;
    private AgentActivityConversationListBinding binding;
    private AgentConversationListRenderer renderer;
    private Optional<AgentConversationListController> controller =
            Optional.empty();
    private boolean createPending;
    private Optional<String> openedConversation = Optional.empty();

    public static Intent intent(Context context) {
        return new Intent(
                context,
                AgentConversationListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AgentPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AgentActivityConversationListBinding.inflate(
                getLayoutInflater());
        AgentConversationAdapter adapter =
                new AgentConversationAdapter(conversation ->
                        open(conversation.id()));
        renderer = new AgentConversationListRenderer(
                binding,
                adapter);
        setContentView(binding.getRoot());
        AgentInsets.applySystemBars(binding.getRoot());
        binding.agentConversationList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.agentConversationList.setAdapter(adapter);
        binding.agentListToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.agentListRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgentConversationListController::refresh));
        binding.agentListLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgentConversationListController::loadMore));
        binding.agentListRetry.setOnClickListener(
                ignored -> retry());
        binding.agentNewConversation.setOnClickListener(
                ignored -> AgentCreateConversationDialog.show(
                        this,
                        this::create));
    }

    @Override
    protected void onStart() {
        super.onStart();
        openedConversation = Optional.empty();
        Optional<AgentFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(AgentConversationListState.error(
                    unavailable()));
            return;
        }
        AgentFeatureRuntime value = runtime.orElseThrow();
        AgentConversationListController next =
                new AgentConversationListController(
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
        createPending = false;
        openedConversation = Optional.empty();
        super.onStop();
    }

    private void create(String title) {
        createPending = true;
        controller.ifPresent(value -> value.create(title));
    }

    private void retry() {
        controller.ifPresent(value -> {
            if (createPending) {
                value.retryCreate();
            } else {
                value.open();
            }
        });
    }

    private void render(AgentConversationListState state) {
        renderer.render(state);
        if (createPending
                && (state.phase()
                                == AgentConversationListState.Phase.WARNING
                        || state.phase()
                                == AgentConversationListState.Phase.ERROR)
                && state.failure()
                        .map(AgentText::retryable)
                        .orElse(false)) {
            binding.agentListRetry.setVisibility(View.VISIBLE);
        }
        if (createPending
                && state.failure().isPresent()
                && !AgentText.retryable(
                        state.failure().orElseThrow())) {
            createPending = false;
        }
        state.created().ifPresent(conversation -> {
            createPending = false;
            open(conversation.id());
        });
    }

    private void open(String conversationId) {
        if (openedConversation
                .filter(conversationId::equals)
                .isPresent()) {
            return;
        }
        openedConversation = Optional.of(conversationId);
        startActivity(AgentConversationActivity.intent(
                this,
                conversationId));
    }

    private Optional<AgentFeatureRuntime> runtime() {
        return getApplication()
                        instanceof AgentRuntimeProvider provider
                ? provider.agentRuntime()
                : Optional.empty();
    }

    private static AgentException unavailable() {
        return new AgentException(
                AgentFailureKind.ACCESS_REVOKED,
                "Agent runtime is unavailable.");
    }
}
