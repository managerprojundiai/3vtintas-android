package br.com.tresvtintas.mobile.core.systemconfiguration;

@FunctionalInterface
public interface SystemConfigurationStateListener {
    void onSystemConfigurationStateChanged(SystemConfigurationState state);
}
