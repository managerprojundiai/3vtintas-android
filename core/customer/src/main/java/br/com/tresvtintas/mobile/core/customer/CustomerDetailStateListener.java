package br.com.tresvtintas.mobile.core.customer;

@FunctionalInterface
public interface CustomerDetailStateListener {
    void onCustomerDetailStateChanged(CustomerDetailState state);
}
