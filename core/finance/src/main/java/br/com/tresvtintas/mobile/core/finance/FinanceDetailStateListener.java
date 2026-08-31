package br.com.tresvtintas.mobile.core.finance;

@FunctionalInterface
public interface FinanceDetailStateListener {
    void onFinanceDetailStateChanged(FinanceDetailState state);
}
