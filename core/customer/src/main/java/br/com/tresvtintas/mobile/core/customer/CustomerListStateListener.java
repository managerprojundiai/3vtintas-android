package br.com.tresvtintas.mobile.core.customer;

@FunctionalInterface
public interface CustomerListStateListener {
    void onCustomerListStateChanged(CustomerListState state);
}
