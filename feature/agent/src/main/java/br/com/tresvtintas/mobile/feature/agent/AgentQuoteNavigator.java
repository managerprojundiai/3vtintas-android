package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;

@FunctionalInterface
public interface AgentQuoteNavigator {
    void open(Context context, long quoteId);
}
