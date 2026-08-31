package br.com.tresvtintas.mobile.feature.systemconfiguration;

import android.view.View;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationState;
import br.com.tresvtintas.mobile.feature.systemconfiguration.databinding.SystemConfigurationActivityBinding;

final class SystemConfigurationRenderer {
    private final SystemConfigurationActivityBinding binding;
    private int renderedRevision;

    SystemConfigurationRenderer(SystemConfigurationActivityBinding binding) {
        this.binding = binding;
    }

    void render(SystemConfigurationState state) {
        boolean busy = state.phase() == SystemConfigurationState.Phase.LOADING
                || state.phase() == SystemConfigurationState.Phase.SAVING;
        binding.systemConfigurationProgress.setVisibility(
                busy ? View.VISIBLE : View.GONE);
        binding.systemConfigurationSave.setEnabled(!busy
                && state.configuration().isPresent());
        binding.systemConfigurationRetry.setEnabled(!busy);
        binding.systemConfigurationForm.setEnabled(!busy);
        if (state.configuration().isPresent()) {
            Snapshot snapshot = state.configuration().orElseThrow();
            if (snapshot.revision() != renderedRevision
                    || state.phase() == SystemConfigurationState.Phase.READY) {
                bind(snapshot);
            }
            binding.systemConfigurationRevision.setText(
                    binding.getRoot().getResources().getString(
                            R.string.system_configuration_revision,
                            snapshot.revision()));
        }
    }

    Values values(String normalizedCommissionRate) {
        return new Values(
                text(binding.systemConfigurationStoreName),
                text(binding.systemConfigurationStorePhone),
                text(binding.systemConfigurationStoreAddress),
                text(binding.systemConfigurationLaborCompany),
                text(binding.systemConfigurationLaborContact),
                normalizedCommissionRate,
                binding.systemConfigurationAutoApprove.isChecked());
    }

    String commissionRate() {
        return text(binding.systemConfigurationCommissionRate);
    }

    private void bind(Snapshot snapshot) {
        Values values = snapshot.values();
        binding.systemConfigurationStoreName.setText(values.storeName());
        binding.systemConfigurationStorePhone.setText(values.storePhone());
        binding.systemConfigurationStoreAddress.setText(values.storeAddress());
        binding.systemConfigurationLaborCompany.setText(values.laborCompanyName());
        binding.systemConfigurationLaborContact.setText(
                values.laborCompanyContact());
        binding.systemConfigurationCommissionRate.setText(
                values.defaultCommissionRate());
        binding.systemConfigurationAutoApprove.setChecked(
                values.autoApprovePainters());
        renderedRevision = snapshot.revision();
    }

    private static String text(android.widget.EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
