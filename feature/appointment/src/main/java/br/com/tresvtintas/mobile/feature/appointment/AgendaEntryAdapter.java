package br.com.tresvtintas.mobile.feature.appointment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.appointment.AgendaEntry;
import br.com.tresvtintas.mobile.core.appointment.AgendaEntryType;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentItemAgendaEntryBinding;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

final class AgendaEntryAdapter
        extends RecyclerView.Adapter<AgendaEntryAdapter.Holder> {
    private final Consumer<AgendaEntry> onSelected;
    private final ZoneId zoneId;
    private final NumberFormat money =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final DateTimeFormatter time =
            DateTimeFormatter.ofPattern("HH:mm", new Locale("pt", "BR"));
    private List<AgendaEntry> entries = List.of();

    AgendaEntryAdapter(
            ZoneId zoneId,
            Consumer<AgendaEntry> onSelected) {
        this.zoneId = Objects.requireNonNull(
                zoneId,
                "Agenda timezone is required.");
        this.onSelected = Objects.requireNonNull(
                onSelected,
                "Agenda entry listener is required.");
        setHasStableIds(true);
    }

    void submit(List<AgendaEntry> value) {
        if (value == null || value.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Agenda entries are invalid.");
        }
        List<AgendaEntry> next = List.copyOf(value);
        List<AgendaEntry> previous = entries;
        DiffUtil.DiffResult difference = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return previous.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return next.size();
                    }

                    @Override
                    public boolean areItemsTheSame(
                            int oldPosition,
                            int newPosition) {
                        AgendaEntry oldEntry = previous.get(oldPosition);
                        AgendaEntry newEntry = next.get(newPosition);
                        return oldEntry.source() == newEntry.source()
                                && oldEntry.sourceId() == newEntry.sourceId();
                    }

                    @Override
                    public boolean areContentsTheSame(
                            int oldPosition,
                            int newPosition) {
                        return previous.get(oldPosition).equals(
                                next.get(newPosition));
                    }
                });
        entries = next;
        difference.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        AgendaEntry entry = entries.get(position);
        return entry.source().ordinal() * 1_000_000_000L + entry.sourceId();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(AppointmentItemAgendaEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final AppointmentItemAgendaEntryBinding binding;

        Holder(AppointmentItemAgendaEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AgendaEntry entry) {
            binding.agendaEntryType.setText(type(entry.type()));
            binding.agendaEntryTitle.setText(entry.title());
            binding.agendaEntryMeta.setText(binding.getRoot()
                    .getContext()
                    .getString(
                            R.string.agenda_entry_meta,
                            time.format(entry.occursAt().atZone(zoneId)),
                            AgendaText.status(
                                    binding.getRoot().getContext(),
                                    entry.status())));
            entry.amount().ifPresentOrElse(
                    amount -> {
                        binding.agendaEntryAmount.setText(
                                money.format(amount));
                        binding.agendaEntryAmount.setVisibility(View.VISIBLE);
                    },
                    () -> binding.agendaEntryAmount.setVisibility(View.GONE));
            binding.agendaEntryCard.setOnClickListener(
                    ignored -> onSelected.accept(entry));
        }

        private int type(AgendaEntryType type) {
            return switch (type) {
                case APPOINTMENT -> R.string.agenda_entry_appointment;
                case DELIVERY -> R.string.agenda_entry_delivery;
                case COLLECTION -> R.string.agenda_entry_collection;
                case RECEIVABLE -> R.string.agenda_entry_receivable;
                case PAYABLE -> R.string.agenda_entry_payable;
                case EXPENSE -> R.string.agenda_entry_expense;
            };
        }
    }
}
