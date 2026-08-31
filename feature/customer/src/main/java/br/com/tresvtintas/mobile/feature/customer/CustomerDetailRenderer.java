package br.com.tresvtintas.mobile.feature.customer;

import android.view.View;
import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailState;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerActivityDetailBinding;

final class CustomerDetailRenderer {
    private final CustomerActivityDetailBinding binding;
    private boolean writeAllowed;

    CustomerDetailRenderer(CustomerActivityDetailBinding binding) {
        this.binding = binding;
    }

    void writeAllowed(boolean value) {
        writeAllowed = value;
    }

    void render(CustomerDetailState state) {
        binding.customerDetailProgress.setVisibility(
                state.phase() == CustomerDetailState.Phase.LOADING
                        ? View.VISIBLE
                        : View.INVISIBLE);
        binding.customerDetailContent.setVisibility(View.GONE);
        binding.customerDetailErrorGroup.setVisibility(View.GONE);
        if (state.phase() == CustomerDetailState.Phase.READY) {
            show(state.customer().orElseThrow());
        } else if (state.phase() == CustomerDetailState.Phase.ERROR) {
            binding.customerDetailError.setText(CustomerFailureText.resource(
                    state.failure().orElseThrow()));
            state.requestId().ifPresent(requestId ->
                    binding.customerDetailError.append(
                            "\n\n" + binding.getRoot().getContext().getString(
                                    R.string.customer_support_code,
                                    requestId)));
            binding.customerDetailErrorGroup.setVisibility(View.VISIBLE);
        }
    }

    private void show(CustomerDetail customer) {
        binding.customerDetailName.setText(customer.name());
        binding.customerDetailPhone.setText(field(
                R.string.customer_phone_label,
                customer.phone().orElse(null)));
        binding.customerDetailEmail.setText(field(
                R.string.customer_email_label,
                customer.email().orElse(null)));
        binding.customerDetailCpf.setText(field(
                R.string.customer_cpf_label,
                customer.cpf().orElse(null)));
        String location = location(customer);
        binding.customerDetailAddress.setText(field(
                R.string.customer_address_label,
                location));
        binding.customerDetailNotes.setText(customer.notes().orElseGet(() ->
                binding.getRoot().getContext().getString(
                        R.string.customer_not_informed)));
        binding.customerDetailEdit.setVisibility(
                writeAllowed ? View.VISIBLE : View.GONE);
        binding.customerDetailContent.setVisibility(View.VISIBLE);
    }

    private String field(int label, String value) {
        String display = value == null || value.isBlank()
                ? binding.getRoot().getContext().getString(
                        R.string.customer_not_informed)
                : value;
        return binding.getRoot().getContext().getString(
                R.string.customer_field_value,
                binding.getRoot().getContext().getString(label),
                display);
    }

    private String location(CustomerDetail customer) {
        String address = customer.address().orElse("");
        String city = customer.city().orElse("");
        String state = customer.state().orElse("");
        String cityState = city;
        if (!city.isBlank() && !state.isBlank()) {
            cityState = binding.getRoot().getContext().getString(
                    R.string.customer_city_state_value,
                    city,
                    state);
        } else if (city.isBlank()) {
            cityState = state;
        }
        if (!address.isBlank() && !cityState.isBlank()) {
            return address + "\n" + cityState;
        }
        return !address.isBlank() ? address : cityState;
    }
}
