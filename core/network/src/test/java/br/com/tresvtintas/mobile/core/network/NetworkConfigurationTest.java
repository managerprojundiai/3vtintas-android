package br.com.tresvtintas.mobile.core.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NetworkConfigurationTest {
    @Test
    public void acceptsTlsMobileApiBaseUrl() {
        NetworkConfiguration configuration = new NetworkConfiguration(
                "https://www.3vtintas.com.br/api/mobile/v1/", "0.2.0-auth-foundation", 3);

        assertEquals(
                "Validated base URL must be retained.",
                "https://www.3vtintas.com.br/api/mobile/v1/",
                configuration.baseUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPublicPlaintextTransport() {
        new NetworkConfiguration(
                "http://www.3vtintas.com.br/api/mobile/v1/", "0.2.0", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWrongApiPath() {
        new NetworkConfiguration("https://www.3vtintas.com.br/", "0.2.0", 3);
    }
}
