package br.com.tresvtintas.mobile.core.commission;

@FunctionalInterface
public interface CommissionDetailStateListener {
    void onCommissionDetailStateChanged(CommissionDetailState state);
}
