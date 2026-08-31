package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.agent.AgentConversationDetailController;
import br.com.tresvtintas.mobile.core.agent.AgentConversationDetailState;
import br.com.tresvtintas.mobile.core.agent.AgentDocument;
import br.com.tresvtintas.mobile.core.agent.AgentEventSubscription;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentMessageRole;
import br.com.tresvtintas.mobile.core.agent.AgentTurnCoordinator;
import br.com.tresvtintas.mobile.core.agent.AgentTurnState;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentActivityConversationBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Optional;

public final class AgentConversationActivity
        extends AppCompatActivity {
    private static final String EXTRA_CONVERSATION_ID =
            "br.com.tresvtintas.mobile.agent.CONVERSATION_ID";
    private final AgentConversationDetailController.Listener
            detailListener = this::renderDetail;
    private final AgentTurnCoordinator.Listener turnListener =
            new AgentTurnCoordinator.Listener() {
                @Override
                public void onAgentTurnStateChanged(
                        AgentTurnState state) {
                    renderTurn(state);
                }

                @Override
                public void onAgentHistoryInvalidated(
                        String invalidatedConversationId) {
                    if (conversationId().filter(
                            invalidatedConversationId::equals)
                            .isPresent()) {
                        controller.ifPresent(
                                AgentConversationDetailController::refresh);
                    }
                }
            };
    private AgentActivityConversationBinding binding;
    private AgentConversationDetailRenderer detailRenderer;
    private AgentTurnRenderer turnRenderer;
    private AgentMessageAdapter messageAdapter;
    private AgentActionUiCoordinator actionUi;
    private Optional<AgentConversationDetailController> controller =
            Optional.empty();
    private Optional<AgentTurnCoordinator> turnCoordinator =
            Optional.empty();
    private Optional<AgentEventSubscription> observation =
            Optional.empty();
    private boolean clearDraftWhenAccepted;

    public static Intent intent(
            Context context,
            String conversationId) {
        return new Intent(context, AgentConversationActivity.class)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AgentPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AgentActivityConversationBinding.inflate(
                getLayoutInflater());
        messageAdapter = new AgentMessageAdapter(
                this::openDocument,
                this::openCreatedQuote,
                action -> actionUi.review(action));
        detailRenderer = new AgentConversationDetailRenderer(
                binding,
                messageAdapter);
        turnRenderer = new AgentTurnRenderer(binding);
        actionUi = new AgentActionUiCoordinator(
                this,
                binding.getRoot(),
                messageAdapter,
                this::openCreatedQuote,
                this::refreshHistory);
        setContentView(binding.getRoot());
        AgentInsets.applySystemBars(binding.getRoot());
        LinearLayoutManager messages =
                new LinearLayoutManager(this);
        messages.setStackFromEnd(true);
        binding.agentMessageList.setLayoutManager(messages);
        binding.agentMessageList.setAdapter(messageAdapter);
        binding.agentDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.agentDetailRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgentConversationDetailController::refresh));
        binding.agentDetailRetry.setOnClickListener(
                ignored -> openConversation());
        binding.agentDetailLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgentConversationDetailController::loadMore));
        binding.agentSend.setOnClickListener(
                ignored -> send());
        binding.agentRetrySend.setOnClickListener(
                ignored -> turnCoordinator.ifPresent(
                        AgentTurnCoordinator::retrySend));
        binding.agentCancelTurn.setOnClickListener(
                ignored -> confirmCancellation());
        actionUi.restore(state);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        actionUi.save(state);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AgentFeatureRuntime> runtime = runtime();
        Optional<String> conversation = conversationId();
        if (runtime.isEmpty() || conversation.isEmpty()) {
            renderUnavailable();
            return;
        }
        AgentFeatureRuntime value = runtime.orElseThrow();
        String requiredConversation = conversation.orElseThrow();
        AgentConversationDetailController next =
                new AgentConversationDetailController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(detailListener);
        actionUi.start(value);
        turnCoordinator = Optional.of(value.turnCoordinator());
        try {
            observation = Optional.of(
                    value.turnCoordinator().observe(
                            requiredConversation,
                            turnListener));
            next.open(requiredConversation);
        } catch (IllegalArgumentException failure) {
            stopRuntime();
            renderUnavailable();
        }
    }

    @Override
    protected void onStop() {
        stopRuntime();
        clearDraftWhenAccepted = false;
        binding.agentInput.setText("");
        super.onStop();
    }

    private void send() {
        Optional<String> conversation = conversationId();
        if (conversation.isEmpty() || turnCoordinator.isEmpty()) {
            return;
        }
        try {
            String message =
                    br.com.tresvtintas.mobile.core.agent.AgentText
                            .message(String.valueOf(
                                    binding.agentInput.getText()));
            binding.agentInputLayout.setError(null);
            clearDraftWhenAccepted = true;
            turnCoordinator.orElseThrow().send(
                    conversation.orElseThrow(),
                    message);
        } catch (IllegalArgumentException failure) {
            clearDraftWhenAccepted = false;
            binding.agentInputLayout.setError(
                    getString(R.string.agent_input_invalid));
        }
    }

    private void renderDetail(
            AgentConversationDetailState state) {
        detailRenderer.render(state);
        state.snapshot().ifPresent(snapshot -> {
            snapshot.pendingTurnId().ifPresent(turnId ->
                    turnCoordinator.ifPresent(value ->
                            value.recover(
                                    snapshot.conversation().id(),
                                    Optional.of(turnId))));
            acknowledgeConfirmedHistory(snapshot);
        });
    }

    private void renderTurn(AgentTurnState state) {
        turnRenderer.render(state);
        if (clearDraftWhenAccepted
                && state.turn().isPresent()
                && state.phase()
                        != AgentTurnState.Phase.SUBMITTING
                && state.phase() != AgentTurnState.Phase.ERROR) {
            binding.agentInput.setText("");
            binding.agentInputLayout.setError(null);
            clearDraftWhenAccepted = false;
        }
    }

    private void acknowledgeConfirmedHistory(
            AgentConversationDetailState.Snapshot snapshot) {
        turnCoordinator.ifPresent(coordinator -> {
            AgentTurnState state = coordinator.currentState();
            state.turn().filter(turn ->
                            state.terminal()
                                    && snapshot.messages().stream()
                                            .anyMatch(message ->
                                                    message.role()
                                                                    == AgentMessageRole
                                                                            .ASSISTANT
                                                            && message.turnId()
                                                                    .filter(turn.id()::equals)
                                                                    .isPresent()))
                    .ifPresent(turn -> coordinator.acknowledgeHistory(
                            snapshot.conversation().id()));
        });
    }

    private void confirmCancellation() {
        if (turnCoordinator
                .map(AgentTurnCoordinator::currentState)
                .filter(AgentTurnState::canCancel)
                .isEmpty()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.agent_cancel_confirmation_title)
                .setMessage(R.string.agent_cancel_confirmation_message)
                .setNegativeButton(
                        R.string.agent_cancel_dialog,
                        null)
                .setPositiveButton(
                        R.string.agent_cancel_confirmation_action,
                        (dialog, which) -> turnCoordinator.ifPresent(
                                AgentTurnCoordinator::cancel))
                .show();
    }

    private void refreshHistory() {
        controller.ifPresent(
                AgentConversationDetailController::refresh);
    }

    private void openConversation() {
        Optional<String> conversation = conversationId();
        if (conversation.isPresent() && controller.isPresent()) {
            controller.orElseThrow().open(
                    conversation.orElseThrow());
        }
    }

    private void openDocument(
            Context context,
            AgentDocument document) {
        Optional<AgentFeatureRuntime> available = runtime();
        if (available.isEmpty()) {
            renderUnavailable();
            return;
        }
        available.orElseThrow()
                .documentNavigator()
                .open(context, document);
    }

    private void openCreatedQuote(
            Context context,
            long quoteId) {
        Optional<AgentFeatureRuntime> available = runtime();
        if (available.isEmpty()) {
            renderUnavailable();
            return;
        }
        available.orElseThrow()
                .quoteNavigator()
                .open(context, quoteId);
    }

    private void stopRuntime() {
        observation.ifPresent(AgentEventSubscription::close);
        observation = Optional.empty();
        controller.ifPresent(value -> {
            value.unsubscribe(detailListener);
            value.close();
        });
        controller = Optional.empty();
        actionUi.stop();
        turnCoordinator = Optional.empty();
    }

    private void renderUnavailable() {
        AgentException failure = new AgentException(
                AgentFailureKind.ACCESS_REVOKED,
                "Agent detail runtime is unavailable.");
        detailRenderer.render(
                AgentConversationDetailState.error(failure));
        turnRenderer.render(AgentTurnState.closed());
    }

    private Optional<String> conversationId() {
        return Optional.ofNullable(
                        getIntent().getStringExtra(
                                EXTRA_CONVERSATION_ID))
                .filter(value -> !value.isBlank());
    }

    private Optional<AgentFeatureRuntime> runtime() {
        return getApplication()
                        instanceof AgentRuntimeProvider provider
                ? provider.agentRuntime()
                : Optional.empty();
    }
}
