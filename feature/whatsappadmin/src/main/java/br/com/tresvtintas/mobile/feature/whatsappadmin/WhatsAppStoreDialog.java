package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Connection;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Store;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppProvider;
import br.com.tresvtintas.mobile.feature.whatsappadmin.databinding.WhatsAppAdminStoreDialogBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class WhatsAppStoreDialog {
    interface Listener {
        void configureMeta(long storeId, String phoneNumberId, Optional<String> phoneNumber);

        void provisionEvolution(long storeId);

        void setMode(long storeId, WhatsAppChannelMode mode);

        void refreshEvolution(long connectionId);

        void requestQr(long connectionId);
    }

    private static final List<WhatsAppChannelMode> MODES = List.of(
            WhatsAppChannelMode.DISABLED,
            WhatsAppChannelMode.META,
            WhatsAppChannelMode.EVOLUTION,
            WhatsAppChannelMode.BOTH);
    private final Context context;
    private final Listener listener;
    private Optional<AlertDialog> dialog = Optional.empty();
    private Optional<WhatsAppAdminStoreDialogBinding> binding = Optional.empty();
    private Optional<Store> current = Optional.empty();

    WhatsAppStoreDialog(Context context, Listener listener) {
        this.context = Objects.requireNonNull(context, "Context is required.");
        this.listener = Objects.requireNonNull(listener, "Listener is required.");
    }

    void show(Store store) {
        dismiss();
        WhatsAppAdminStoreDialogBinding createdBinding =
                WhatsAppAdminStoreDialogBinding.inflate(
                LayoutInflater.from(context));
        createdBinding.mode.setAdapter(new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                modeLabels()));
        AlertDialog createdDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.whatsapp_admin_manage_title)
                .setView(createdBinding.getRoot())
                .setNegativeButton(R.string.whatsapp_admin_close, null)
                .create();
        createdDialog.setOnDismissListener(ignored -> clear());
        binding = Optional.of(createdBinding);
        dialog = Optional.of(createdDialog);
        bindActions();
        update(store);
        createdDialog.show();
    }

    void update(Store store) {
        if (binding.isEmpty() || dialog.isEmpty()) {
            return;
        }
        if (current.isPresent() && current.orElseThrow().id() != store.id()) {
            return;
        }
        current = Optional.of(store);
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        views.storeName.setText(store.name());
        views.storeStatus.setText(WhatsAppAdministrationText.storeStatus(
                context,
                store.status()));
        views.mode.setSelection(MODES.indexOf(store.mode()));
        bindMeta(store.connection(WhatsAppProvider.META_CLOUD));
        bindEvolution(store.connection(WhatsAppProvider.EVOLUTION));
    }

    void dismiss() {
        dialog.ifPresent(AlertDialog::dismiss);
        clear();
    }

    private void bindActions() {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        views.saveMode.setOnClickListener(ignored -> saveMode());
        views.configureMeta.setOnClickListener(ignored -> configureMeta());
        views.provisionEvolution.setOnClickListener(
                ignored -> confirm(
                        R.string.whatsapp_admin_confirm_provision_title,
                        context.getString(
                                R.string.whatsapp_admin_confirm_provision_message,
                                store().name()),
                        () -> listener.provisionEvolution(store().id())));
        views.refreshEvolution.setOnClickListener(ignored ->
                store().connection(WhatsAppProvider.EVOLUTION).ifPresent(connection ->
                        confirm(
                                R.string.whatsapp_admin_confirm_refresh_title,
                                context.getString(
                                        R.string.whatsapp_admin_confirm_refresh_message,
                                        store().name()),
                                () -> listener.refreshEvolution(connection.id()))));
        views.requestQr.setOnClickListener(ignored ->
                store().connection(WhatsAppProvider.EVOLUTION).ifPresent(connection ->
                        confirm(
                                R.string.whatsapp_admin_confirm_qr_title,
                                context.getString(
                                        R.string.whatsapp_admin_confirm_qr_message,
                                        store().name()),
                                () -> listener.requestQr(connection.id()))));
    }

    private void saveMode() {
        WhatsAppChannelMode selected = MODES.get(
                binding.orElseThrow().mode.getSelectedItemPosition());
        if (selected == store().mode()) {
            return;
        }
        confirm(
                R.string.whatsapp_admin_confirm_mode_title,
                context.getString(
                        R.string.whatsapp_admin_confirm_mode_message,
                        store().name(),
                        WhatsAppAdministrationText.mode(context, store().mode()),
                        WhatsAppAdministrationText.mode(context, selected)),
                () -> listener.setMode(store().id(), selected));
    }

    private void configureMeta() {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        String phoneNumberId = views.metaPhoneNumberId.getText()
                .toString().strip();
        String phoneNumber = views.metaPhoneNumber.getText()
                .toString().strip();
        if (!phoneNumberId.matches("^\\d{5,120}$")
                || !phoneNumber.isEmpty() && !phoneNumber.matches("^\\d{10,15}$")) {
            views.metaValidation.setVisibility(View.VISIBLE);
            return;
        }
        views.metaValidation.setVisibility(View.GONE);
        confirm(
                R.string.whatsapp_admin_confirm_meta_title,
                context.getString(
                        R.string.whatsapp_admin_confirm_meta_message,
                        store().name(),
                        phoneNumberId),
                () -> listener.configureMeta(
                        store().id(),
                        phoneNumberId,
                        phoneNumber.isEmpty()
                                ? Optional.empty()
                                : Optional.of(phoneNumber)));
    }

    private void bindMeta(Optional<Connection> connection) {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        if (connection.isEmpty()) {
            views.metaStatus.setText(R.string.whatsapp_admin_meta_not_configured);
            views.metaPhoneNumberId.setText("");
            views.metaPhoneNumber.setText("");
            return;
        }
        connection.ifPresent(this::bindMetaConnection);
    }

    private void bindMetaConnection(Connection value) {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        views.metaStatus.setText(WhatsAppAdministrationText.connectionStatus(
                context,
                value.status()));
        views.metaPhoneNumberId.setText(value.phoneNumberId().orElse(""));
        views.metaPhoneNumber.setText(value.phoneNumber().orElse(""));
    }

    private void bindEvolution(Optional<Connection> connection) {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        boolean configured = connection.isPresent();
        views.provisionEvolution.setVisibility(configured ? View.GONE : View.VISIBLE);
        views.refreshEvolution.setVisibility(configured ? View.VISIBLE : View.GONE);
        views.requestQr.setVisibility(configured ? View.VISIBLE : View.GONE);
        if (!configured) {
            views.evolutionStatus.setText(
                    R.string.whatsapp_admin_evolution_not_configured);
            views.evolutionInstance.setText(
                    R.string.whatsapp_admin_evolution_instance_missing);
            return;
        }
        connection.ifPresent(this::bindEvolutionConnection);
    }

    private void bindEvolutionConnection(Connection value) {
        WhatsAppAdminStoreDialogBinding views = binding.orElseThrow();
        views.evolutionStatus.setText(WhatsAppAdministrationText.connectionStatus(
                context,
                value.status()));
        views.evolutionInstance.setText(value.evolutionInstanceName().orElse(
                context.getString(R.string.whatsapp_admin_evolution_instance_missing)));
    }

    private void confirm(int titleResource, String message, Runnable action) {
        new AlertDialog.Builder(context)
                .setTitle(titleResource)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.whatsapp_admin_confirm,
                        (ignored, which) -> action.run())
                .show();
    }

    private void clear() {
        dialog = Optional.empty();
        binding = Optional.empty();
        current = Optional.empty();
    }

    private List<String> modeLabels() {
        List<String> labels = new ArrayList<>(MODES.size());
        for (WhatsAppChannelMode mode : MODES) {
            labels.add(WhatsAppAdministrationText.mode(context, mode));
        }
        return labels;
    }

    private Store store() {
        return current.orElseThrow();
    }
}
