package br.com.tresvtintas.mobile.feature.appointment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.appointment.AppointmentSummary;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentItemSummaryBinding;
import java.util.ArrayList;
import java.util.List;

final class AppointmentSummaryAdapter extends ListAdapter<
        AppointmentSummary,
        AppointmentSummaryAdapter.Holder> {
    private final OnSelected listener;

    AppointmentSummaryAdapter(OnSelected listener) {
        super(new Difference());
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(AppointmentItemSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AppointmentItemSummaryBinding binding;

        Holder(AppointmentItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppointmentSummary item, OnSelected listener) {
            binding.appointmentItemTitle.setText(item.title());
            binding.appointmentItemStatus.setText(
                    AppointmentText.status(
                            binding.getRoot().getContext(),
                            item.status()));
            binding.appointmentItemSchedule.setText(
                    binding.getRoot().getContext().getString(
                            R.string.appointment_context,
                            AppointmentText.dateTime(item.scheduledAt()),
                            AppointmentText.duration(
                                    binding.getRoot().getContext(),
                                    item.durationMinutes())));
            binding.appointmentItemResponsible.setText(
                    binding.getRoot().getContext().getString(
                            R.string.appointment_responsible,
                            AppointmentText.person(
                                    binding.getRoot().getContext(),
                                    item.responsible())));
            List<String> context = new ArrayList<>();
            context.add(AppointmentText.kind(
                    binding.getRoot().getContext(),
                    item.kind()));
            item.organization().ifPresent(value -> context.add(value.name()));
            item.customer().ifPresent(value -> context.add(value.name()));
            item.location().ifPresent(context::add);
            binding.appointmentItemContext.setText(
                    String.join(" • ", context));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onSelected(item.id()));
        }
    }

    @FunctionalInterface
    interface OnSelected {
        void onSelected(long appointmentId);
    }

    private static final class Difference
            extends DiffUtil.ItemCallback<AppointmentSummary> {
        @Override
        public boolean areItemsTheSame(
                @NonNull AppointmentSummary oldItem,
                @NonNull AppointmentSummary newItem) {
            return oldItem.id() == newItem.id();
        }

        @Override
        public boolean areContentsTheSame(
                @NonNull AppointmentSummary oldItem,
                @NonNull AppointmentSummary newItem) {
            return oldItem.equals(newItem);
        }
    }
}
