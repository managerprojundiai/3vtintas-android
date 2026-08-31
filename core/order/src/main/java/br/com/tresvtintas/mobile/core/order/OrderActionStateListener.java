package br.com.tresvtintas.mobile.core.order;

@FunctionalInterface
public interface OrderActionStateListener {
    void onOrderActionStateChanged(OrderActionState state);
}
