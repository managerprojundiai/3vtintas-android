package br.com.tresvtintas.mobile.core.order;

@FunctionalInterface
public interface OrderListStateListener {
    void onOrderListStateChanged(OrderListState state);
}
