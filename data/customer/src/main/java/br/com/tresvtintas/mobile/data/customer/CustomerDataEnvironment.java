package br.com.tresvtintas.mobile.data.customer;

import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.data.customer.remote.RetrofitCustomerRemote;
import java.util.Objects;

public final class CustomerDataEnvironment {
    private CustomerDataEnvironment() {
        throw new AssertionError("No instances.");
    }

    public static RemoteCustomerRepository repository(
            CustomerAccountScope scope,
            MobileApi protectedApi) {
        return new RemoteCustomerRepository(
                Objects.requireNonNull(
                        scope,
                        "Customer account scope is required."),
                new RetrofitCustomerRemote(Objects.requireNonNull(
                        protectedApi,
                        "Protected mobile API is required.")));
    }
}
