package br.com.tresvtintas.mobile.feature.agent;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogCreateConversationBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class AgentCreateConversationDialog {
    @FunctionalInterface
    interface Listener {
        void onCreateRequested(String title);
    }

    private AgentCreateConversationDialog() {
        throw new AssertionError("No instances.");
    }

    static void show(Activity activity, Listener listener) {
        if (activity == null || listener == null) {
            throw new IllegalArgumentException(
                    "Agent create dialog dependencies are required.");
        }
        AgentDialogCreateConversationBinding binding =
                AgentDialogCreateConversationBinding.inflate(
                        activity.getLayoutInflater());
        binding.agentCreateTitle.setText(
                R.string.agent_create_dialog_default);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.agent_create_dialog_title)
                .setView(binding.getRoot())
                .setNegativeButton(
                        R.string.agent_cancel_dialog,
                        null)
                .setPositiveButton(
                        R.string.agent_create_dialog_confirm,
                        null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(button -> {
                            try {
                                String title =
                                        br.com.tresvtintas.mobile.core.agent
                                                .AgentText.title(
                                                        String.valueOf(
                                                                binding.agentCreateTitle
                                                                        .getText()));
                                binding.agentCreateTitleLayout
                                        .setError(null);
                                listener.onCreateRequested(title);
                                binding.agentCreateTitle.setText("");
                                dialog.dismiss();
                            } catch (IllegalArgumentException failure) {
                                binding.agentCreateTitleLayout.setError(
                                        activity.getString(
                                                R.string
                                                        .agent_create_invalid));
                            }
                        }));
        dialog.show();
    }
}
