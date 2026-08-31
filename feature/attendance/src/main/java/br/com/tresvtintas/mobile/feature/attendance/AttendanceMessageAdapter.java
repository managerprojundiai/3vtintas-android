package br.com.tresvtintas.mobile.feature.attendance;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessageDirection;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceItemMessageBinding;

final class AttendanceMessageAdapter
        extends ListAdapter<
                AttendanceMessage,
                AttendanceMessageAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AttendanceMessage>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AttendanceMessage oldItem,
                        @NonNull AttendanceMessage newItem) {
                    return oldItem.id().equals(newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AttendanceMessage oldItem,
                        @NonNull AttendanceMessage newItem) {
                    return oldItem.equals(newItem);
                }
            };

    AttendanceMessageAdapter() {
        super(DIFFERENCE);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(AttendanceItemMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AttendanceItemMessageBinding binding;

        Holder(AttendanceItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AttendanceMessage value) {
            var context = binding.getRoot().getContext();
            boolean outbound =
                    value.direction()
                            == AttendanceMessageDirection.OUTBOUND;
            binding.attendanceMessageRow.setGravity(
                    outbound ? Gravity.END : Gravity.START);
            binding.attendanceMessageDirection.setText(
                    outbound
                            ? R.string.attendance_message_outbound
                            : R.string.attendance_message_inbound);
            binding.attendanceMessageContent.setText(value.content());
            binding.attendanceMessageTime.setText(
                    AttendanceText.activity(value.createdAt()));
            binding.attendanceMessageTruncated.setVisibility(
                    value.truncated()
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE);
            binding.getRoot().setContentDescription(
                    context.getString(
                            R.string.attendance_message_accessibility,
                            binding.attendanceMessageDirection.getText(),
                            value.content(),
                            binding.attendanceMessageTime.getText()));
        }
    }
}
