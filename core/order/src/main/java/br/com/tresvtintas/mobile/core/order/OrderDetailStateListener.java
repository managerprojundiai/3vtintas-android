package br.com.tresvtintas.mobile.core.order;

@FunctionalInterface
public interface OrderDetailStateListener {
    void onOrderDetailStateChanged(OrderDetailState state);
}
