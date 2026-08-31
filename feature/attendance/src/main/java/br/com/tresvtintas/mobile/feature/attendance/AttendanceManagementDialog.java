package br.com.tresvtintas.mobile.feature.attendance;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssignee;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementState;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceDialogManagementBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Consumer;

final class AttendanceManagementDialog {
    private final Context context;

    AttendanceManagementDialog(Context context) {
        this.context = context;
    }

    void show(
            AttendanceManagementState.Snapshot current,
            Consumer<AttendanceManagementSelection> onSave) {
        AttendanceDialogManagementBinding binding =
                AttendanceDialogManagementBinding.inflate(
                        LayoutInflater.from(context));
        MutableSelection selection = configure(binding, current);
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.attendance_management_title)
                .setView(binding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.attendance_management_save,
                        (ignored, which) -> onSave.accept(
                                selection.toValue()))
                .show();
    }

    private MutableSelection configure(
            AttendanceDialogManagementBinding dialog,
            AttendanceManagementState.Snapshot current) {
        List<AttendanceFolder> folders =
                Arrays.asList(AttendanceFolder.values());
        List<AttendancePriority> priorities =
                Arrays.asList(AttendancePriority.values());
        List<AssignmentOption> assignments = assignments(current);
        MutableSelection selection = new MutableSelection(
                current.folder(),
                current.priority(),
                current.selection().assignedToUserId());

        dialog.attendanceManagementFolder.setAdapter(
                adapter(folderLabels(folders)));
        dialog.attendanceManagementFolder.setText(
                AttendanceText.folder(context, current.folder()),
                false);
        dialog.attendanceManagementFolder.setOnItemClickListener(
                (parent, view, position, id) ->
                        selection.folder = folders.get(position));

        dialog.attendanceManagementPriority.setAdapter(
                adapter(priorityLabels(priorities)));
        dialog.attendanceManagementPriority.setText(
                AttendanceText.priority(context, current.priority()),
                false);
        dialog.attendanceManagementPriority.setOnItemClickListener(
                (parent, view, position, id) ->
                        selection.priority = priorities.get(position));

        dialog.attendanceManagementAssignee.setAdapter(
                adapter(assignmentLabels(assignments)));
        int assignmentIndex = assignmentIndex(
                assignments,
                selection.assignedToUserId);
        dialog.attendanceManagementAssignee.setText(
                assignments.get(assignmentIndex).label(),
                false);
        dialog.attendanceManagementAssignee.setOnItemClickListener(
                (parent, view, position, id) ->
                        selection.assignedToUserId =
                                assignments.get(position).userId());
        return selection;
    }

    private ArrayAdapter<String> adapter(List<String> values) {
        return new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                values);
    }

    private List<String> folderLabels(
            List<AttendanceFolder> folders) {
        List<String> result = new ArrayList<>(folders.size());
        for (AttendanceFolder value : folders) {
            result.add(AttendanceText.folder(context, value));
        }
        return List.copyOf(result);
    }

    private List<String> priorityLabels(
            List<AttendancePriority> priorities) {
        List<String> result = new ArrayList<>(priorities.size());
        for (AttendancePriority value : priorities) {
            result.add(AttendanceText.priority(context, value));
        }
        return List.copyOf(result);
    }

    private static List<String> assignmentLabels(
            List<AssignmentOption> assignments) {
        List<String> result = new ArrayList<>(assignments.size());
        for (AssignmentOption value : assignments) {
            result.add(value.label());
        }
        return List.copyOf(result);
    }

    private List<AssignmentOption> assignments(
            AttendanceManagementState.Snapshot current) {
        List<AssignmentOption> result = new ArrayList<>();
        result.add(new AssignmentOption(
                OptionalLong.empty(),
                context.getString(R.string.attendance_unassigned)));
        current.assignedUser().ifPresent(value -> {
            boolean eligible = current.assignees().stream()
                    .anyMatch(item -> item.id() == value.id());
            if (!eligible) {
                result.add(new AssignmentOption(
                        OptionalLong.of(value.id()),
                        value.name().orElseGet(() ->
                                context.getString(
                                        R.string
                                                .attendance_management_user_id,
                                        value.id()))));
            }
        });
        for (AttendanceAssignee item : current.assignees()) {
            result.add(new AssignmentOption(
                    OptionalLong.of(item.id()),
                    item.displayName()));
        }
        return List.copyOf(result);
    }

    private static int assignmentIndex(
            List<AssignmentOption> assignments,
            OptionalLong selected) {
        for (int index = 0; index < assignments.size(); index++) {
            if (assignments.get(index).userId().equals(selected)) {
                return index;
            }
        }
        return 0;
    }

    private record AssignmentOption(
            OptionalLong userId,
            String label) {
    }

    private static final class MutableSelection {
        private AttendanceFolder folder;
        private AttendancePriority priority;
        private OptionalLong assignedToUserId;

        MutableSelection(
                AttendanceFolder folder,
                AttendancePriority priority,
                OptionalLong assignedToUserId) {
            this.folder = folder;
            this.priority = priority;
            this.assignedToUserId = assignedToUserId;
        }

        AttendanceManagementSelection toValue() {
            return new AttendanceManagementSelection(
                    folder,
                    priority,
                    assignedToUserId);
        }
    }
}
