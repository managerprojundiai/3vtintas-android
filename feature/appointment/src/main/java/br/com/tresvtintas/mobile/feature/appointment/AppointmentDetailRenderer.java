package br.com.tresvtintas.mobile.feature.appointment;

import android.view.View;
import br.com.tresvtintas.mobile.core.appointment.AppointmentAction;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetailState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationState;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityDetailBinding;

final class AppointmentDetailRenderer {
    private final AppointmentActivityDetailBinding binding;

    AppointmentDetailRenderer(
            AppointmentActivityDetailBinding binding) {
        this.binding = binding;
    }

    void renderDetail(AppointmentDetailState state) {
        boolean loading =
                state.phase() == AppointmentDetailState.Phase.LOADING;
        binding.appointmentDetailProgress.setVisibility(
                loading ? View.VISIBLE : View.INVISIBLE);
        binding.appointmentDetailErrorGroup.setVisibility(View.GONE);
        if (state.phase() == AppointmentDetailState.Phase.ERROR) {
            binding.appointmentDetailContent.setVisibility(View.GONE);
            binding.appointmentDetailErrorGroup.setVisibility(View.VISIBLE);
            binding.appointmentDetailError.setText(
                    AppointmentText.failure(state.failure().orElseThrow()));
            state.requestId().ifPresent(value ->
                    binding.appointmentDetailError.append(
                            "\n\n"
                                    + binding.getRoot().getContext().getString(
                                            R.string.appointment_support_code,
                                            value)));
            return;
        }
        state.detail().ifPresent(this::detail);
    }

    void renderMutation(AppointmentMutationState state) {
        boolean busy =
                state.phase() == AppointmentMutationState.Phase.RUNNING;
        binding.appointmentDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.appointmentDetailEdit.setEnabled(!busy);
        binding.appointmentDetailConfirm.setEnabled(!busy);
        binding.appointmentDetailComplete.setEnabled(!busy);
        binding.appointmentDetailCancel.setEnabled(!busy);
        if (state.phase() == AppointmentMutationState.Phase.ERROR) {
            binding.appointmentDetailNotice.setText(
                    AppointmentText.failure(state.failure().orElseThrow()));
            state.requestId().ifPresent(value ->
                    binding.appointmentDetailNotice.append(
                            "\n"
                                    + binding.getRoot().getContext().getString(
                                            R.string.appointment_support_code,
                                            value)));
            binding.appointmentDetailNoticeCard.setVisibility(View.VISIBLE);
        }
    }

    private void detail(AppointmentDetail value) {
        var item = value.summary();
        binding.appointmentDetailContent.setVisibility(View.VISIBLE);
        binding.appointmentDetailTitleText.setText(item.title());
        binding.appointmentDetailStatus.setText(
                AppointmentText.status(
                        binding.getRoot().getContext(),
                        item.status()));
        binding.appointmentDetailSchedule.setText(
                binding.getRoot().getContext().getString(
                        R.string.appointment_context,
                        AppointmentText.dateTime(item.scheduledAt()),
                        AppointmentText.duration(
                                binding.getRoot().getContext(),
                                item.durationMinutes())));
        binding.appointmentDetailKind.setText(
                AppointmentText.kind(
                        binding.getRoot().getContext(),
                        item.kind()));
        binding.appointmentDetailResponsible.setText(
                binding.getRoot().getContext().getString(
                        R.string.appointment_responsible,
                        AppointmentText.person(
                                binding.getRoot().getContext(),
                                item.responsible())));
        optional(
                binding.appointmentDetailOrganization,
                item.organization().map(organization ->
                        binding.getRoot().getContext().getString(
                                R.string.appointment_organization,
                                organization.name())).orElse(null));
        optional(
                binding.appointmentDetailCustomer,
                item.customer().map(customer ->
                        binding.getRoot().getContext().getString(
                                R.string.appointment_customer,
                                customer.name())).orElse(null));
        optional(
                binding.appointmentDetailLocation,
                item.location().map(location ->
                        binding.getRoot().getContext().getString(
                                R.string.appointment_location,
                                location)).orElse(null));
        optional(
                binding.appointmentDetailOrder,
                item.order().map(order ->
                        binding.getRoot().getContext().getString(
                                R.string.appointment_order,
                                order.id(),
                                order.status())).orElse(null));
        binding.appointmentDetailDescription.setText(
                value.description().orElseGet(() ->
                        binding.getRoot().getContext().getString(
                                R.string.appointment_no_description)));
        binding.appointmentDetailEdit.setVisibility(
                visible(item.allowedActions().contains(
                        AppointmentAction.UPDATE)));
        binding.appointmentDetailConfirm.setVisibility(
                visible(item.allowedActions().contains(
                        AppointmentAction.CONFIRM)));
        binding.appointmentDetailComplete.setVisibility(
                visible(item.allowedActions().contains(
                        AppointmentAction.COMPLETE)));
        binding.appointmentDetailCancel.setVisibility(
                visible(item.allowedActions().contains(
                        AppointmentAction.CANCEL)));
        binding.appointmentDetailNoticeCard.setVisibility(
                item.kind() == AppointmentKind.DELIVERY
                        ? View.VISIBLE
                        : View.GONE);
        if (item.kind() == AppointmentKind.DELIVERY) {
            binding.appointmentDetailNotice.setText(
                    R.string.appointment_delivery_read_only);
        }
    }

    private static void optional(
            android.widget.TextView view,
            String value) {
        view.setVisibility(value == null ? View.GONE : View.VISIBLE);
        if (value != null) {
            view.setText(value);
        }
    }

    private static int visible(boolean value) {
        return value ? View.VISIBLE : View.GONE;
    }
}
