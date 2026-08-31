package br.com.tresvtintas.mobile.core.bootstrap;

@FunctionalInterface
public interface BootstrapStateListener {
    void onBootstrapStateChanged(BootstrapState state);
}
