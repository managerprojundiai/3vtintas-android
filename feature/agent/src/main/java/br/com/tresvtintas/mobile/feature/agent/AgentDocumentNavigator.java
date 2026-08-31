package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import br.com.tresvtintas.mobile.core.agent.AgentDocument;

@FunctionalInterface
public interface AgentDocumentNavigator {
    void open(Context context, AgentDocument document);
}
