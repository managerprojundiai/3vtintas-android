package br.com.tresvtintas.mobile.feature.useradmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationException;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationListController;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationListState;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationStatus;
import br.com.tresvtintas.mobile.feature.useradmin.databinding.UserAdminActivityListBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class UserAdministrationListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final UserAdministrationListController.Listener listener = this::render;
    private UserAdminActivityListBinding binding;
    private UserSummaryAdapter adapter;
    private Optional<UserAdministrationListController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private UserAdministrationListState current = UserAdministrationListState.empty();
    private List<Organization> organizations = List.of();
    private boolean filtersBound;

    public static Intent intent(Context context) {
        return new Intent(context, UserAdministrationListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        UserAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = UserAdminActivityListBinding.inflate(getLayoutInflater());
        adapter = new UserSummaryAdapter(this::openUser);
        setContentView(binding.getRoot());
        UserAdministrationInsets.applySystemBars(binding.getRoot());
        binding.userAdminList.setLayoutManager(new LinearLayoutManager(this));
        binding.userAdminList.setAdapter(adapter);
        binding.userAdminToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.userAdminRefresh.setOnClickListener(
                ignored -> controller.ifPresent(UserAdministrationListController::refresh));
        binding.userAdminRetry.setOnClickListener(
                ignored -> controller.ifPresent(UserAdministrationListController::open));
        binding.userAdminLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(UserAdministrationListController::loadMore));
        binding.userAdminSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<UserAdministrationFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(UserAdministrationListState.error(new UserAdministrationException(
                    UserAdministrationFailureKind.ACCESS_REVOKED,
                    "User administration runtime is unavailable.")));
            return;
        }
        UserAdministrationFeatureRuntime value = runtime.orElseThrow();
        UserAdministrationListController next = new UserAdministrationListController(
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

    private void render(UserAdministrationListState state) {
        current = state;
        boolean busy = state.phase() == UserAdministrationListState.Phase.LOADING
                || state.phase() == UserAdministrationListState.Phase.REFRESHING
                || state.phase() == UserAdministrationListState.Phase.LOADING_MORE;
        binding.userAdminProgress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        binding.userAdminRefresh.setEnabled(!busy);
        binding.userAdminLoadMore.setEnabled(!busy);
        if (state.snapshot().isPresent()) {
            UserAdministrationListState.Snapshot snapshot = state.snapshot().orElseThrow();
            adapter.submit(snapshot.users());
            binding.userAdminCount.setText(getResources().getQuantityString(
                    R.plurals.user_admin_count,
                    snapshot.users().size(),
                    snapshot.users().size()));
            binding.userAdminList.setVisibility(
                    snapshot.users().isEmpty() ? View.GONE : View.VISIBLE);
            binding.userAdminEmpty.setVisibility(
                    snapshot.users().isEmpty() ? View.VISIBLE : View.GONE);
            binding.userAdminLoadMore.setVisibility(
                    snapshot.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
            binding.userAdminErrorGroup.setVisibility(View.GONE);
            if (!filtersBound || !organizations.equals(snapshot.options().organizations())) {
                bindFilters(snapshot.options().organizations());
            }
        } else if (state.phase() == UserAdministrationListState.Phase.ERROR) {
            adapter.submit(List.of());
            binding.userAdminList.setVisibility(View.GONE);
            binding.userAdminEmpty.setVisibility(View.GONE);
            binding.userAdminLoadMore.setVisibility(View.GONE);
            binding.userAdminErrorGroup.setVisibility(View.VISIBLE);
            binding.userAdminError.setText(failureText(state.failure().orElseThrow()));
        } else {
            binding.userAdminList.setVisibility(View.GONE);
            binding.userAdminEmpty.setVisibility(View.GONE);
            binding.userAdminErrorGroup.setVisibility(View.GONE);
        }
        binding.userAdminNoticeCard.setVisibility(
                state.phase() == UserAdministrationListState.Phase.STALE
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void bindFilters(List<Organization> values) {
        organizations = List.copyOf(values);
        List<String> organizationLabels = new ArrayList<>();
        organizationLabels.add(getString(R.string.user_admin_all_organizations));
        values.stream().map(Organization::name).forEach(organizationLabels::add);
        binding.userAdminOrganization.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                organizationLabels));
        binding.userAdminOrganization.setText(organizationLabels.get(0), false);
        binding.userAdminOrganization.setOnItemClickListener(
                (parent, view, position, id) -> updateOrganization(position));

        List<String> roleLabels = new ArrayList<>();
        roleLabels.add(getString(R.string.user_admin_all_roles));
        for (AppRole role : AppRole.values()) {
            roleLabels.add(getString(UserAdministrationText.role(role)));
        }
        binding.userAdminRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                roleLabels));
        binding.userAdminRole.setText(roleLabels.get(0), false);
        binding.userAdminRole.setOnItemClickListener(
                (parent, view, position, id) -> current.snapshot().ifPresent(snapshot ->
                        controller.ifPresent(value -> value.apply(snapshot.query().withRole(
                                position < 1
                                        ? Optional.empty()
                                        : Optional.of(AppRole.values()[position - 1]))))));

        List<String> statusLabels = List.of(
                getString(R.string.user_admin_all_statuses),
                getString(R.string.user_admin_status_active),
                getString(R.string.user_admin_status_blocked));
        binding.userAdminStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                statusLabels));
        binding.userAdminStatus.setText(statusLabels.get(0), false);
        binding.userAdminStatus.setOnItemClickListener(
                (parent, view, position, id) -> current.snapshot().ifPresent(snapshot ->
                        controller.ifPresent(value -> value.apply(snapshot.query().withStatus(
                                position == 1
                                        ? Optional.of(UserAdministrationStatus.ACTIVE)
                                        : position == 2
                                                ? Optional.of(UserAdministrationStatus.BLOCKED)
                                                : Optional.empty())))));
        filtersBound = true;
    }

    private void updateOrganization(int position) {
        OptionalLong organizationId = position < 1
                ? OptionalLong.empty()
                : OptionalLong.of(organizations.get(position - 1).id());
        current.snapshot().ifPresent(snapshot -> controller.ifPresent(
                value -> value.apply(snapshot.query().withOrganization(organizationId))));
    }

    private void openUser(long userId) {
        startActivity(UserAdministrationDetailActivity.intent(this, userId));
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
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
                controller.ifPresent(item -> item.apply(snapshot.query().withSearch(value))));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private String failureText(UserAdministrationException failure) {
        String message = getString(UserAdministrationText.failure(failure.kind()));
        return failure.requestId().map(value -> message + "\n\n" + getString(
                R.string.user_admin_support_code,
                value)).orElse(message);
    }

    private Optional<UserAdministrationFeatureRuntime> runtime() {
        return getApplication() instanceof UserAdministrationRuntimeProvider provider
                ? provider.userAdministrationRuntime()
                : Optional.empty();
    }
}
