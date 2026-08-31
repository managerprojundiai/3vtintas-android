package br.com.tresvtintas.mobile.core.catalog;

@FunctionalInterface
public interface CatalogStateListener {
    void onCatalogStateChanged(CatalogState state);
}
