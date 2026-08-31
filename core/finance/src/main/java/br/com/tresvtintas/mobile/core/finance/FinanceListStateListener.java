package br.com.tresvtintas.mobile.core.finance;

@FunctionalInterface
public interface FinanceListStateListener {
    void onFinanceListStateChanged(FinanceListState state);
}
