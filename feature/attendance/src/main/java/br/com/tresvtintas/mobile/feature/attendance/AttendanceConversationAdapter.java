package br.com.tresvtintas.mobile.feature.attendance;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceItemConversationBinding;

final class AttendanceConversationAdapter
        extends ListAdapter<
                AttendanceConversation,
                AttendanceConversationAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AttendanceConversation>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AttendanceConversation oldItem,
                        @NonNull AttendanceConversation newItem) {
                    return oldItem.id().equals(newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AttendanceConversation oldItem,
                        @NonNull AttendanceConversation newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final ConversationListener listener;

    AttendanceConversationAdapter(ConversationListener listener) {
        super(DIFFERENCE);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(AttendanceItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false),
                listener);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AttendanceItemConversationBinding binding;
        private final ConversationListener listener;

        Holder(
                AttendanceItemConversationBinding binding,
                ConversationListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(AttendanceConversation value) {
            var context = binding.getRoot().getContext();
            binding.attendanceItemTitle.setText(
                    value.customer()
                            .displayName()
                            .filter(name -> !name.isBlank())
                            .orElseGet(() -> context.getString(
                                    R.string.attendance_customer_unknown)));
            String organization = value.organization()
                    .map(AttendanceConversation.Organization::name)
                    .orElseGet(() -> context.getString(
                            R.string.attendance_organization_global));
            binding.attendanceItemChannel.setText(context.getString(
                    R.string.attendance_item_channel,
                    AttendanceText.channel(context, value.channel()),
                    organization));
            binding.attendanceItemPreview.setText(value.lastMessage()
                    .map(AttendanceConversation.LastMessage::preview)
                    .filter(preview -> !preview.isBlank())
                    .orElseGet(() -> context.getString(
                            R.string.attendance_no_preview)));
            String owner = value.assignedUser()
                    .flatMap(AttendanceConversation.AssignedUser::name)
                    .filter(name -> !name.isBlank())
                    .orElseGet(() -> context.getString(
                            R.string.attendance_unassigned));
            binding.attendanceItemOwner.setText(context.getString(
                    R.string.attendance_item_owner,
                    owner,
                    AttendanceText.mode(context, value.handlingMode())));
            binding.attendanceItemMeta.setText(
                    context.getResources().getQuantityString(
                            R.plurals.attendance_item_meta,
                            value.stats().unreadCount(),
                            AttendanceText.folder(
                                    context,
                                    value.folder()),
                            AttendanceText.priority(
                                    context,
                                    value.priority()),
                            value.stats().unreadCount(),
                            AttendanceText.activity(
                                    value.activityAt())));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onConversationSelected(value));
        }
    }

    @FunctionalInterface
    interface ConversationListener {
        void onConversationSelected(AttendanceConversation conversation);
    }
}
