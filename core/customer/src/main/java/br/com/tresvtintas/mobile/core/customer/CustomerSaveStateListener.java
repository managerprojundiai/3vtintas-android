package br.com.tresvtintas.mobile.core.customer;

@FunctionalInterface
public interface CustomerSaveStateListener {
    void onCustomerSaveStateChanged(CustomerSaveState state);
}
