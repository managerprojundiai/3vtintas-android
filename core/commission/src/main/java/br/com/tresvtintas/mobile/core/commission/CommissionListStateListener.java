package br.com.tresvtintas.mobile.core.commission;

@FunctionalInterface
public interface CommissionListStateListener {
    void onCommissionListStateChanged(CommissionListState state);
}
