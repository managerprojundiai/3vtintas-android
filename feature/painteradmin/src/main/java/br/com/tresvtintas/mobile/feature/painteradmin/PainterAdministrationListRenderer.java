package br.com.tresvtintas.mobile.feature.painteradmin;

import android.view.View;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListState;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListState.Snapshot;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminActivityListBinding;

final class PainterAdministrationListRenderer {
    enum Tab {
        PAINTERS,
        REQUESTS
    }

    private final PainterAdminActivityListBinding binding;
    private final PainterSummaryAdapter painterAdapter;
    private final AccessRequestSummaryAdapter requestAdapter;
    private Tab tab = Tab.PAINTERS;

    PainterAdministrationListRenderer(
            PainterAdminActivityListBinding binding,
            PainterSummaryAdapter painterAdapter,
            AccessRequestSummaryAdapter requestAdapter) {
        this.binding = binding;
        this.painterAdapter = painterAdapter;
        this.requestAdapter = requestAdapter;
    }

    Tab tab() {
        return tab;
    }

    void tab(Tab value, PainterAdministrationListState state) {
        tab = value;
        binding.painterAdminList.setAdapter(
                value == Tab.PAINTERS ? painterAdapter : requestAdapter);
        render(state);
    }

    void render(PainterAdministrationListState state) {
        boolean busy = state.phase()
                == PainterAdministrationListState.Phase.LOADING
                || state.phase()
                == PainterAdministrationListState.Phase.REFRESHING
                || state.phase()
                == PainterAdministrationListState.Phase.LOADING_MORE;
        binding.painterAdminProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.painterAdminRefresh.setEnabled(!busy);
        binding.painterAdminLoadMore.setEnabled(!busy);
        binding.painterAdminAdd.setEnabled(!busy);
        binding.painterAdminFilters.setVisibility(
                tab == Tab.PAINTERS ? View.VISIBLE : View.GONE);
        binding.painterAdminAdd.setVisibility(
                tab == Tab.PAINTERS ? View.VISIBLE : View.GONE);
        if (state.snapshot().isPresent()) {
            showSnapshot(state.snapshot().orElseThrow());
            binding.painterAdminErrorGroup.setVisibility(View.GONE);
        } else if (state.phase()
                == PainterAdministrationListState.Phase.ERROR) {
            showFailure(state.failure().orElseThrow());
        } else {
            binding.painterAdminList.setVisibility(View.GONE);
            binding.painterAdminEmpty.setVisibility(View.GONE);
            binding.painterAdminErrorGroup.setVisibility(View.GONE);
        }
        if (state.phase() == PainterAdministrationListState.Phase.STALE) {
            binding.painterAdminNotice.setText(R.string.painter_admin_stale);
            binding.painterAdminNoticeCard.setVisibility(View.VISIBLE);
        } else {
            binding.painterAdminNoticeCard.setVisibility(View.GONE);
        }
    }

    private void showSnapshot(Snapshot snapshot) {
        int count;
        boolean empty;
        boolean hasMore;
        if (tab == Tab.PAINTERS) {
            painterAdapter.submit(snapshot.painters());
            count = snapshot.painters().size();
            empty = snapshot.painters().isEmpty();
            hasMore = snapshot.painterCursor().isPresent();
            binding.painterAdminEmpty.setText(
                    R.string.painter_admin_empty_painters);
        } else {
            requestAdapter.submit(snapshot.requests());
            count = snapshot.requests().size();
            empty = snapshot.requests().isEmpty();
            hasMore = snapshot.requestCursor().isPresent();
            binding.painterAdminEmpty.setText(
                    R.string.painter_admin_empty_requests);
        }
        binding.painterAdminCount.setText(
                binding.getRoot().getResources().getQuantityString(
                        R.plurals.painter_admin_count,
                        count,
                        count));
        binding.painterAdminList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.painterAdminEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.painterAdminLoadMore.setVisibility(
                hasMore ? View.VISIBLE : View.GONE);
    }

    private void showFailure(PainterAdministrationException failure) {
        binding.painterAdminList.setVisibility(View.GONE);
        binding.painterAdminEmpty.setVisibility(View.GONE);
        binding.painterAdminLoadMore.setVisibility(View.GONE);
        binding.painterAdminErrorGroup.setVisibility(View.VISIBLE);
        String message = binding.getRoot().getContext().getString(
                PainterAdministrationText.failure(failure.kind()));
        if (failure.requestId().isPresent()) {
            message += "\n\n" + binding.getRoot().getContext().getString(
                    R.string.painter_admin_support_code,
                    failure.requestId().orElseThrow());
        }
        binding.painterAdminError.setText(message);
    }
}
