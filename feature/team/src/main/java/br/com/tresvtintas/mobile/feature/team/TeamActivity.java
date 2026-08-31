package br.com.tresvtintas.mobile.feature.team;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.team.TeamController;
import br.com.tresvtintas.mobile.core.team.TeamException;
import br.com.tresvtintas.mobile.core.team.TeamFailureKind;
import br.com.tresvtintas.mobile.core.team.TeamState;
import br.com.tresvtintas.mobile.core.team.TeamStateListener;
import br.com.tresvtintas.mobile.feature.team.databinding.TeamActivityBinding;
import java.util.Optional;

public final class TeamActivity extends AppCompatActivity {
    private final TeamStateListener listener = this::render;
    private TeamActivityBinding binding;
    private TeamRenderer renderer;
    private Optional<TeamController> controller = Optional.empty();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TeamPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = TeamActivityBinding.inflate(getLayoutInflater());
        TeamMemberAdapter adapter = new TeamMemberAdapter();
        renderer = new TeamRenderer(binding, adapter);
        setContentView(binding.getRoot());
        TeamInsets.applySystemBars(binding.getRoot());
        binding.teamMembers.setLayoutManager(
                new LinearLayoutManager(this));
        binding.teamMembers.setAdapter(adapter);
        binding.teamToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.teamRefresh.setOnClickListener(
                ignored -> controller.ifPresent(TeamController::refresh));
        binding.teamRetry.setOnClickListener(
                ignored -> controller.ifPresent(TeamController::load));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<TeamFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(TeamState.error(new TeamException(
                    TeamFailureKind.ACCESS_REVOKED,
                    "Team runtime is unavailable.")));
            return;
        }
        TeamFeatureRuntime value = runtime.orElseThrow();
        TeamController next = new TeamController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.load();
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

    private void render(TeamState state) {
        renderer.render(state);
    }

    private Optional<TeamFeatureRuntime> runtime() {
        return getApplication() instanceof TeamRuntimeProvider provider
                ? provider.teamRuntime()
                : Optional.empty();
    }
}
