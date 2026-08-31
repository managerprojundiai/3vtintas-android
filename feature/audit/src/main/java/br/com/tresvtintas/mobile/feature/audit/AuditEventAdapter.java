package br.com.tresvtintas.mobile.feature.audit;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Actor;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import br.com.tresvtintas.mobile.feature.audit.databinding.AuditItemBinding;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

final class AuditEventAdapter extends RecyclerView.Adapter<AuditEventAdapter.Holder> {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());
    private List<Event> items = List.of();

    void submit(List<Event> values) {
        int previousSize = items.size();
        items = List.of();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        items = List.copyOf(values);
        if (!items.isEmpty()) {
            notifyItemRangeInserted(0, items.size());
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(AuditItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.bind(items.get(position), dateFormatter);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AuditItemBinding binding;

        Holder(AuditItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Event event, DateTimeFormatter formatter) {
            binding.auditItemAction.setText(event.action());
            binding.auditItemEntity.setText(event.entity().orElse(
                    binding.getRoot().getContext().getString(
                            R.string.audit_system_entity)));
            binding.auditItemTime.setText(formatter.format(event.occurredAt()));
            binding.auditItemActor.setText(event.actor()
                    .map(this::actorLabel)
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.audit_system_actor)));
        }

        private String actorLabel(Actor actor) {
            return binding.getRoot().getContext().getString(
                    R.string.audit_actor_format,
                    actor.name(),
                    binding.getRoot().getContext().getString(
                            AuditText.role(actor.role())));
        }
    }
}
