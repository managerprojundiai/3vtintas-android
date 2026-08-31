package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Connection;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Store;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppProvider;
import br.com.tresvtintas.mobile.feature.whatsappadmin.databinding.WhatsAppAdminStoreItemBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class WhatsAppStoreAdapter
        extends RecyclerView.Adapter<WhatsAppStoreAdapter.Holder> {
    @FunctionalInterface
    interface Listener {
        void onStoreSelected(Store store);
    }

    private final List<Store> items = new ArrayList<>();
    private final Listener listener;

    WhatsAppStoreAdapter(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "Listener is required.");
    }

    void replace(List<Store> stores) {
        int previousSize = items.size();
        items.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        items.addAll(stores);
        if (!items.isEmpty()) {
            notifyItemRangeInserted(0, items.size());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(WhatsAppAdminStoreItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final WhatsAppAdminStoreItemBinding binding;

        Holder(WhatsAppAdminStoreItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Store store) {
            binding.storeName.setText(store.name());
            binding.storeSlug.setText(store.slug());
            binding.storeStatus.setText(WhatsAppAdministrationText.storeStatus(
                    binding.getRoot().getContext(),
                    store.status()));
            binding.storeMode.setText(WhatsAppAdministrationText.mode(
                    binding.getRoot().getContext(),
                    store.mode()));
            binding.metaStatus.setText(providerSummary(
                    store.connection(WhatsAppProvider.META_CLOUD),
                    R.string.whatsapp_admin_meta_not_configured));
            binding.evolutionStatus.setText(providerSummary(
                    store.connection(WhatsAppProvider.EVOLUTION),
                    R.string.whatsapp_admin_evolution_not_configured));
            binding.manageAction.setOnClickListener(
                    ignored -> listener.onStoreSelected(store));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onStoreSelected(store));
        }

        private String providerSummary(
                Optional<Connection> connection,
                int missingResource) {
            return connection.map(value -> WhatsAppAdministrationText.connectionStatus(
                    binding.getRoot().getContext(),
                    value.status())).orElseGet(() ->
                    binding.getRoot().getContext().getString(missingResource));
        }
    }
}
