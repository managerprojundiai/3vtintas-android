package br.com.tresvtintas.mobile.feature.audit;

import android.view.View;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditState;
import br.com.tresvtintas.mobile.feature.audit.databinding.AuditActivityBinding;
import java.util.List;

final class AuditRenderer {
    private final AuditActivityBinding binding;
    private final AuditEventAdapter adapter;

    AuditRenderer(AuditActivityBinding binding, AuditEventAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AuditState state) {
        boolean busy = state.phase() == AuditState.Phase.LOADING
                || state.phase() == AuditState.Phase.REFRESHING
                || state.phase() == AuditState.Phase.LOADING_MORE;
        binding.auditProgress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        binding.auditRefresh.setEnabled(!busy);
        binding.auditApplyFilters.setEnabled(!busy);
        binding.auditClearFilters.setEnabled(!busy);
        binding.auditLoadMore.setEnabled(!busy);
        if (state.snapshot().isPresent()) {
            AuditState.Snapshot snapshot = state.snapshot().orElseThrow();
            adapter.submit(snapshot.events());
            binding.auditCount.setText(binding.getRoot().getResources().getQuantityString(
                    R.plurals.audit_count,
                    snapshot.events().size(),
                    snapshot.events().size()));
            binding.auditList.setVisibility(
                    snapshot.events().isEmpty() ? View.GONE : View.VISIBLE);
            binding.auditEmpty.setVisibility(
                    snapshot.events().isEmpty() ? View.VISIBLE : View.GONE);
            binding.auditLoadMore.setVisibility(
                    snapshot.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
            binding.auditErrorGroup.setVisibility(View.GONE);
        } else if (state.phase() == AuditState.Phase.ERROR) {
            adapter.submit(List.of());
            binding.auditList.setVisibility(View.GONE);
            binding.auditEmpty.setVisibility(View.GONE);
            binding.auditLoadMore.setVisibility(View.GONE);
            binding.auditErrorGroup.setVisibility(View.VISIBLE);
            binding.auditError.setText(failureText(state.failure().orElseThrow()));
            binding.auditCount.setText(R.string.audit_count_empty);
        } else {
            binding.auditList.setVisibility(View.GONE);
            binding.auditEmpty.setVisibility(View.GONE);
            binding.auditErrorGroup.setVisibility(View.GONE);
        }
        binding.auditStaleCard.setVisibility(
                state.phase() == AuditState.Phase.STALE ? View.VISIBLE : View.GONE);
    }

    private String failureText(AuditException failure) {
        String message = binding.getRoot().getContext().getString(
                AuditText.failure(failure.kind()));
        return failure.requestId().map(value -> message + "\n\n" +
                binding.getRoot().getContext().getString(
                        R.string.audit_support_code,
                        value)).orElse(message);
    }
}
