package br.com.tresvtintas.mobile.feature.delivery;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStop;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryRouteItemStopBinding;

final class DeliveryRouteStopAdapter extends ListAdapter<
        DeliveryRouteStop,
        DeliveryRouteStopAdapter.Holder> {
    private static final DiffUtil.ItemCallback<DeliveryRouteStop> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull DeliveryRouteStop oldItem,
                        @NonNull DeliveryRouteStop newItem) {
                    return oldItem.deliveryId() == newItem.deliveryId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull DeliveryRouteStop oldItem,
                        @NonNull DeliveryRouteStop newItem) {
                    return oldItem.equals(newItem);
                }
            };

    DeliveryRouteStopAdapter() {
        super(DIFFERENCE);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).deliveryId();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(DeliveryRouteItemStopBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final DeliveryRouteItemStopBinding binding;

        Holder(DeliveryRouteItemStopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DeliveryRouteStop stop) {
            var context = binding.getRoot().getContext();
            String title = context.getString(
                    R.string.delivery_route_stop_position_value,
                    stop.position(),
                    stop.orderId());
            String recipient = stop.recipientName().orElseGet(() ->
                    context.getString(
                            R.string.delivery_route_stop_recipient_unknown));
            String status = context.getString(DeliveryRouteText.stopStatus(stop.status()));
            String meta = context.getString(
                    R.string.delivery_route_stop_meta_value,
                    status,
                    DeliveryRouteText.arrival(context, stop.estimatedArrivalAt()),
                    DeliveryRouteText.distance(context, stop.travelDistanceMeters()));
            binding.deliveryRouteStopPosition.setText(title);
            binding.deliveryRouteStopRecipient.setText(recipient);
            binding.deliveryRouteStopAddress.setText(stop.address());
            binding.deliveryRouteStopMeta.setText(meta);
            binding.getRoot().setContentDescription(
                    title + ". " + recipient + ". " + status + ". " + stop.address());
        }
    }
}
