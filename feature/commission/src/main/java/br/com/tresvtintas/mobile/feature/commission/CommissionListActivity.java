package br.com.tresvtintas.mobile.feature.commission;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.commission.CommissionException;
import br.com.tresvtintas.mobile.core.commission.CommissionFailureKind;
import br.com.tresvtintas.mobile.core.commission.CommissionListController;
import br.com.tresvtintas.mobile.core.commission.CommissionListState;
import br.com.tresvtintas.mobile.core.commission.CommissionListStateListener;
import br.com.tresvtintas.mobile.core.commission.CommissionQuery;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionActivityListBinding;
import java.util.Optional;

public final class CommissionListActivity extends AppCompatActivity {
    private static final String EXTRA_SCOPE =
            "br.com.tresvtintas.mobile.commission.SCOPE";
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CommissionListStateListener listener = this::render;
    private CommissionActivityListBinding binding;
    private CommissionListRenderer renderer;
    private CommissionScope scope;
    private Optional<CommissionListController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();

    public static Intent intent(Context context, CommissionScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Commission scope is required.");
        }
        return new Intent(context, CommissionListActivity.class)
                .putExtra(EXTRA_SCOPE, scope.queryValue());
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CommissionPrivacy.protect(this);
        EdgeToEdge.enable(this);
        scope = readScope(getIntent());
        binding = CommissionActivityListBinding.inflate(getLayoutInflater());
        CommissionSummaryAdapter adapter =
                new CommissionSummaryAdapter(this::openDetail);
        renderer = new CommissionListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        CommissionInsets.applySystemBars(binding.getRoot());
        binding.commissionList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.commissionList.setAdapter(adapter);
        binding.commissionToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.commissionToolbar.setTitle(scope == CommissionScope.TEAM
                ? R.string.commission_team_title
                : R.string.commission_self_title);
        binding.commissionToolbar.setSubtitle(scope == CommissionScope.TEAM
                ? R.string.commission_team_subtitle
                : R.string.commission_self_subtitle);
        binding.commissionRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        CommissionListController::refresh));
        binding.commissionRetry.setOnClickListener(ignored -> retry());
        binding.commissionLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        CommissionListController::loadMore));
        binding.commissionStatusFilters.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeStatus(checked.get(0));
                    }
                });
        binding.commissionSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<CommissionFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(CommissionListState.error(new CommissionException(
                    CommissionFailureKind.ACCESS_REVOKED,
                    "Commission runtime is unavailable.")));
            return;
        }
        CommissionFeatureRuntime value = runtime.orElseThrow();
        CommissionQuery initial = CommissionQuery.initial(scope)
                .withOrganization(value.organizationId())
                .withSearch(text(binding.commissionSearch.getText()));
        CommissionListController next = new CommissionListController(
                value.repository(),
                initial,
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(initial);
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

    @Override
    protected void onDestroy() {
        cancelSearch();
        super.onDestroy();
    }

    private void openDetail(long commissionId) {
        startActivity(CommissionDetailActivity.intent(
                this,
                commissionId,
                scope));
    }

    private void retry() {
        controller.ifPresent(value -> value.open(value.currentQuery()));
    }

    private void changeStatus(int id) {
        CommissionStatus status = id == R.id.commission_status_pending
                ? CommissionStatus.PENDING
                : id == R.id.commission_status_approved
                        ? CommissionStatus.APPROVED
                        : id == R.id.commission_status_paid
                                ? CommissionStatus.PAID
                                : id == R.id.commission_status_cancelled
                                        ? CommissionStatus.CANCELLED
                                        : null;
        controller.ifPresent(value ->
                value.open(value.currentQuery().withStatus(status)));
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value,
                    int start,
                    int before,
                    int count) {
                scheduleSearch(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        };
    }

    private void scheduleSearch(String value) {
        cancelSearch();
        Runnable action = () -> controller.ifPresent(item ->
                item.open(item.currentQuery().withSearch(value)));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private void render(CommissionListState state) {
        renderer.render(state);
    }

    private Optional<CommissionFeatureRuntime> runtime() {
        return getApplication() instanceof CommissionRuntimeProvider provider
                ? provider.commissionRuntime()
                : Optional.empty();
    }

    private static CommissionScope readScope(Intent intent) {
        String value = intent == null ? null : intent.getStringExtra(EXTRA_SCOPE);
        if (CommissionScope.TEAM.queryValue().equals(value)) {
            return CommissionScope.TEAM;
        }
        return CommissionScope.SELF;
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
