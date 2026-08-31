package br.com.tresvtintas.mobile.feature.painteradmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListController;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListState;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminActivityListBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class PainterAdministrationListActivity
        extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PainterAdministrationListController.Listener listener =
            this::render;
    private PainterAdminActivityListBinding binding;
    private PainterAdministrationListRenderer renderer;
    private Optional<PainterAdministrationListController> controller =
            Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private PainterAdministrationListState current =
            PainterAdministrationListState.empty();
    private List<Organization> organizations = List.of();
    private boolean filtersBound;

    public static Intent intent(Context context) {
        return new Intent(
                context,
                PainterAdministrationListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PainterAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = PainterAdminActivityListBinding.inflate(
                getLayoutInflater());
        PainterSummaryAdapter painters =
                new PainterSummaryAdapter(this::openPainter);
        AccessRequestSummaryAdapter requests =
                new AccessRequestSummaryAdapter(this::openRequest);
        renderer = new PainterAdministrationListRenderer(
                binding,
                painters,
                requests);
        setContentView(binding.getRoot());
        PainterAdministrationInsets.applySystemBars(binding.getRoot());
        binding.painterAdminList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.painterAdminList.setAdapter(painters);
        binding.painterAdminToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.painterAdminRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        PainterAdministrationListController::refresh));
        binding.painterAdminRetry.setOnClickListener(
                ignored -> controller.ifPresent(
                        PainterAdministrationListController::open));
        binding.painterAdminAdd.setOnClickListener(
                ignored -> startActivity(PainterCreateActivity.intent(this)));
        binding.painterAdminLoadMore.setOnClickListener(
                ignored -> loadMore());
        binding.painterAdminTabs.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (isChecked) {
                        renderer.tab(
                                checkedId == R.id.painter_admin_tab_requests
                                        ? PainterAdministrationListRenderer.Tab.REQUESTS
                                        : PainterAdministrationListRenderer.Tab.PAINTERS,
                                current);
                    }
                });
        binding.painterAdminSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<PainterAdministrationFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(PainterAdministrationListState.error(
                    new PainterAdministrationException(
                            PainterAdministrationFailureKind.ACCESS_REVOKED,
                            "Painter administration runtime is unavailable.")));
            return;
        }
        PainterAdministrationFeatureRuntime value = runtime.orElseThrow();
        PainterAdministrationListController next =
                new PainterAdministrationListController(
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

    @Override
    protected void onDestroy() {
        cancelSearch();
        super.onDestroy();
    }

    private void render(PainterAdministrationListState state) {
        current = state;
        renderer.render(state);
        state.snapshot().ifPresent(snapshot -> {
            if (!filtersBound
                    || !organizations.equals(
                            snapshot.options().organizations())) {
                bindFilters(snapshot.options().organizations());
            }
        });
    }

    private void bindFilters(List<Organization> values) {
        organizations = List.copyOf(values);
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.painter_admin_all_organizations));
        for (Organization organization : values) {
            names.add(organization.name());
        }
        binding.painterAdminOrganization.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                names));
        binding.painterAdminOrganization.setText(names.get(0), false);
        binding.painterAdminOrganization.setOnItemClickListener(
                (parent, view, position, id) -> {
                    OptionalLong organizationId = position < 1
                            ? OptionalLong.empty()
                            : OptionalLong.of(
                                    organizations.get(position - 1).id());
                    current.snapshot().ifPresent(snapshot ->
                            controller.ifPresent(value -> value.apply(
                                    snapshot.query()
                                            .withOrganization(
                                                    organizationId))));
                });
        List<String> statuses = List.of(
                getString(R.string.painter_admin_all_statuses),
                getString(R.string.painter_admin_status_active),
                getString(R.string.painter_admin_status_blocked),
                getString(R.string.painter_admin_status_pending));
        binding.painterAdminStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                statuses));
        binding.painterAdminStatus.setText(statuses.get(0), false);
        binding.painterAdminStatus.setOnItemClickListener(
                (parent, view, position, id) -> {
                    Optional<PainterAdministrationStatus> status = switch (position) {
                        case 1 -> Optional.of(
                                PainterAdministrationStatus.ACTIVE);
                        case 2 -> Optional.of(
                                PainterAdministrationStatus.BLOCKED);
                        case 3 -> Optional.of(
                                PainterAdministrationStatus.PENDING);
                        default -> Optional.empty();
                    };
                    current.snapshot().ifPresent(snapshot ->
                            controller.ifPresent(value -> value.apply(
                                    snapshot.query().withStatus(status))));
                });
        filtersBound = true;
    }

    private void loadMore() {
        controller.ifPresent(value -> {
            if (renderer.tab()
                    == PainterAdministrationListRenderer.Tab.PAINTERS) {
                value.loadMorePainters();
            } else {
                value.loadMoreRequests();
            }
        });
    }

    private void openPainter(long id) {
        startActivity(PainterDetailActivity.intent(this, id));
    }

    private void openRequest(long id) {
        startActivity(AccessRequestDetailActivity.intent(this, id));
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
        Runnable action = () -> current.snapshot().ifPresent(snapshot ->
                controller.ifPresent(item -> item.apply(
                        snapshot.query().withSearch(value))));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private Optional<PainterAdministrationFeatureRuntime> runtime() {
        return getApplication()
                        instanceof PainterAdministrationRuntimeProvider provider
                ? provider.painterAdministrationRuntime()
                : Optional.empty();
    }
}
