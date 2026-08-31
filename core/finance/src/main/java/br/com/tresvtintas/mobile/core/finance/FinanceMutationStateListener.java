package br.com.tresvtintas.mobile.core.finance;

@FunctionalInterface
public interface FinanceMutationStateListener {
    void onFinanceMutationStateChanged(FinanceMutationState state);
}
