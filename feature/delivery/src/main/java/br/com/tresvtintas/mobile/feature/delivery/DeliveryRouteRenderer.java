package br.com.tresvtintas.mobile.feature.delivery;

import android.view.View;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePlan;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteState;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryRouteActivityBinding;
import java.util.List;

final class DeliveryRouteRenderer {
    private final DeliveryRouteActivityBinding binding;
    private final DeliveryRouteStopAdapter adapter;

    DeliveryRouteRenderer(
            DeliveryRouteActivityBinding binding,
            DeliveryRouteStopAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(DeliveryRouteState state) {
        boolean busy = state.phase() == DeliveryRouteState.Phase.LOADING
                || state.phase() == DeliveryRouteState.Phase.REFRESHING;
        binding.deliveryRouteDate.setText(DeliveryRouteText.date(state.serviceDate()));
        binding.deliveryRouteProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.deliveryRouteRefresh.setEnabled(!busy);
        binding.deliveryRouteRetry.setVisibility(View.GONE);
        binding.deliveryRouteNotice.setVisibility(View.GONE);
        if (state.phase() == DeliveryRouteState.Phase.ERROR) {
            showEmpty(DeliveryText.failure(state.failure().orElseThrow()), true);
            support(state);
            return;
        }
        state.page().flatMap(page -> page.current()).ifPresentOrElse(
                this::showRoute,
                () -> showEmpty(R.string.delivery_route_empty_message, false));
    }

    private void showRoute(DeliveryRoutePlan plan) {
        var context = binding.getRoot().getContext();
        binding.deliveryRouteEmptyGroup.setVisibility(View.GONE);
        binding.deliveryRouteSummaryCard.setVisibility(View.VISIBLE);
        binding.deliveryRouteStopsTitle.setVisibility(View.VISIBLE);
        binding.deliveryRouteStops.setVisibility(View.VISIBLE);
        binding.deliveryRouteStatus.setText(DeliveryRouteText.status(plan.status()));
        binding.deliveryRouteSummary.setText(context.getResources().getQuantityString(
                R.plurals.delivery_route_summary_value,
                plan.stops().size(),
                plan.stops().size(),
                DeliveryRouteText.distance(context, plan.totalDistanceMeters()),
                DeliveryRouteText.duration(context, plan.totalTravelDurationSeconds())));
        binding.deliveryRouteOrigin.setText(context.getString(
                R.string.delivery_route_origin_value,
                plan.origin().address()));
        binding.deliveryRouteNavigate.setVisibility(
                plan.nextStop().isPresent() ? View.VISIBLE : View.GONE);
        adapter.submitList(plan.stops());
    }

    private void showEmpty(int message, boolean retry) {
        adapter.submitList(List.of());
        binding.deliveryRouteSummaryCard.setVisibility(View.GONE);
        binding.deliveryRouteStopsTitle.setVisibility(View.GONE);
        binding.deliveryRouteStops.setVisibility(View.GONE);
        binding.deliveryRouteEmptyGroup.setVisibility(View.VISIBLE);
        binding.deliveryRouteEmptyTitle.setText(retry
                ? R.string.delivery_route_title
                : R.string.delivery_route_empty_title);
        binding.deliveryRouteEmptyMessage.setText(message);
        binding.deliveryRouteRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void support(DeliveryRouteState state) {
        state.requestId().ifPresent(value -> {
            binding.deliveryRouteNotice.setText(binding.getRoot().getContext().getString(
                    R.string.delivery_request_id,
                    value));
            binding.deliveryRouteNotice.setVisibility(View.VISIBLE);
        });
    }
}
