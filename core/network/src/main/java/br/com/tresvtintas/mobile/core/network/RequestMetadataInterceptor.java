package br.com.tresvtintas.mobile.core.network;

import java.io.IOException;
import java.util.UUID;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

final class RequestMetadataInterceptor implements Interceptor {
    static final String APP_VERSION_HEADER = "X-3V-App-Version";
    static final String APP_VERSION_CODE_HEADER = "X-3V-App-Version-Code";
    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private final NetworkConfiguration configuration;

    RequestMetadataInterceptor(NetworkConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header(APP_VERSION_HEADER, configuration.appVersion())
                .header(
                        APP_VERSION_CODE_HEADER,
                        Integer.toString(configuration.appVersionCode()))
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
        if (original.header("Accept") == null) {
            builder.header("Accept", "application/json");
        }
        return chain.proceed(builder.build());
    }
}
