package br.com.tresvtintas.mobile.feature.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Channel;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Outcome;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Phase;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.StepOutcome;

final class AgentReplayText {
    private AgentReplayText() {
        throw new AssertionError("No instances.");
    }

    static int channel(Channel value) {
        return switch (value) {
            case WHATSAPP -> R.string.agent_replay_channel_whatsapp;
            case SITE_CHAT -> R.string.agent_replay_channel_site_chat;
            case MOBILE_APP -> R.string.agent_replay_channel_mobile_app;
            case TELEGRAM -> R.string.agent_replay_channel_telegram;
            case INSTAGRAM -> R.string.agent_replay_channel_instagram;
            case MESSENGER -> R.string.agent_replay_channel_messenger;
            case ADMIN_PANEL -> R.string.agent_replay_channel_admin;
            case API -> R.string.agent_replay_channel_api;
            case UNKNOWN -> R.string.agent_replay_channel_unknown;
        };
    }

    static int outcome(Outcome value) {
        return switch (value) {
            case COMPLETED -> R.string.agent_replay_outcome_completed;
            case BLOCKED -> R.string.agent_replay_outcome_blocked;
            case FAILED -> R.string.agent_replay_outcome_failed;
            case INCOMPLETE -> R.string.agent_replay_outcome_incomplete;
        };
    }

    static int phase(Phase value) {
        return switch (value) {
            case RECEIVED -> R.string.agent_replay_phase_received;
            case PLANNED -> R.string.agent_replay_phase_planned;
            case POLICY_CHECKED -> R.string.agent_replay_phase_policy;
            case ACTION_REQUESTED -> R.string.agent_replay_phase_action_requested;
            case ACTION_COMPLETED -> R.string.agent_replay_phase_action_completed;
            case RESPONSE_PREPARED -> R.string.agent_replay_phase_response;
            case HUMAN_HANDOFF -> R.string.agent_replay_phase_handoff;
            case RECOVERED -> R.string.agent_replay_phase_recovered;
            case COMPLETED -> R.string.agent_replay_phase_completed;
            case OTHER -> R.string.agent_replay_phase_other;
        };
    }

    static int stepOutcome(StepOutcome value) {
        return switch (value) {
            case OK -> R.string.agent_replay_step_ok;
            case BLOCKED -> R.string.agent_replay_step_blocked;
            case FAILED -> R.string.agent_replay_step_failed;
            case UNKNOWN -> R.string.agent_replay_step_unknown;
        };
    }
}
