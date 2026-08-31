package br.com.tresvtintas.mobile.core.whatsappadmin;

@FunctionalInterface
public interface WhatsAppAdministrationStateListener {
    void onWhatsAppAdministrationStateChanged(WhatsAppAdministrationState state);
}
