package br.com.tresvtintas.mobile.feature.order;

import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderPaymentMethod;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderDialogCancellationBinding;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderDialogPaymentBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class OrderActionDialogs {
    private final AppCompatActivity activity;

    OrderActionDialogs(AppCompatActivity activity) {
        this.activity = activity;
    }

    void confirmStatus(OrderAction action, Runnable confirmed) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(OrderText.action(action))
                .setMessage(OrderText.actionWarning(action))
                .setPositiveButton(
                        R.string.order_action_confirm_button,
                        (dialog, ignored) -> confirmed.run())
                .setNegativeButton(R.string.order_action_back, null)
                .show();
    }

    void confirmCancellation(Consumer<Optional<String>> confirmed) {
        OrderDialogCancellationBinding binding =
                OrderDialogCancellationBinding.inflate(
                        LayoutInflater.from(activity));
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.order_action_cancel)
                .setMessage(R.string.order_action_cancel_warning)
                .setView(binding.getRoot())
                .setPositiveButton(
                        R.string.order_action_cancel_confirm,
                        (dialog, ignored) -> confirmed.accept(Optional.of(
                                binding.orderCancellationReason.getText()
                                        == null
                                        ? ""
                                        : binding.orderCancellationReason
                                                .getText()
                                                .toString())))
                .setNegativeButton(R.string.order_action_back, null)
                .show();
    }

    void confirmPayment(
            BiConsumer<OrderPaymentMethod, Optional<String>> confirmed) {
        OrderDialogPaymentBinding binding = OrderDialogPaymentBinding.inflate(
                LayoutInflater.from(activity));
        OrderPaymentMethod[] methods = OrderPaymentMethod.values();
        String[] labels = new String[methods.length];
        for (int index = 0; index < methods.length; index++) {
            labels[index] = activity.getString(
                    OrderText.paymentMethod(methods[index]));
        }
        binding.orderPaymentMethod.setAdapter(new ArrayAdapter<>(
                activity,
                android.R.layout.simple_list_item_1,
                labels));
        binding.orderPaymentMethod.setText(labels[0], false);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.order_action_payment)
                .setMessage(R.string.order_action_payment_warning)
                .setView(binding.getRoot())
                .setPositiveButton(
                        R.string.order_action_payment_confirm,
                        (dialog, ignored) -> {
                            int selected = binding.orderPaymentMethod
                                    .getListSelection();
                            if (selected < 0 || selected >= methods.length) {
                                selected = indexOf(
                                        labels,
                                        binding.orderPaymentMethod
                                                .getText()
                                                .toString());
                            }
                            OrderPaymentMethod method = methods[
                                    selected < 0 ? 0 : selected];
                            confirmed.accept(
                                    method,
                                    Optional.of(binding.orderPaymentReference
                                            .getText() == null
                                            ? ""
                                            : binding.orderPaymentReference
                                                    .getText()
                                                    .toString()));
                        })
                .setNegativeButton(R.string.order_action_back, null)
                .show();
    }

    private static int indexOf(String[] values, String selected) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(selected)) {
                return index;
            }
        }
        return -1;
    }
}
