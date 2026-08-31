package br.com.tresvtintas.mobile.core.network;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.time.Duration;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;

/**
 * Creates the single hardened HTTP stack used by the mobile API.
 */
public final class MobileApiFactory {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(20);

    private MobileApiFactory() {
    }

    public static MobileApi create(
            NetworkConfiguration configuration,
            BearerTokenProvider tokenProvider) {
        return create(configuration, tokenProvider, null);
    }

    public static MobileApi create(
            NetworkConfiguration configuration,
            BearerTokenProvider tokenProvider,
            BearerTokenRefresher tokenRefresher) {
        return createClient(
                configuration,
                tokenProvider,
                tokenRefresher).api();
    }

    public static MobileNetworkClient createClient(
            NetworkConfiguration configuration,
            BearerTokenProvider tokenProvider,
            BearerTokenRefresher tokenRefresher) {
        if (configuration == null) {
            throw new IllegalArgumentException("Network configuration is required.");
        }
        ObjectMapper mapper = objectMapper();
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(IO_TIMEOUT)
                .writeTimeout(IO_TIMEOUT)
                .callTimeout(CALL_TIMEOUT)
                .addInterceptor(new RequestMetadataInterceptor(configuration))
                .addInterceptor(new BearerAuthenticationInterceptor(
                        tokenProvider, tokenRefresher));
        if (tokenRefresher != null) {
            clientBuilder.authenticator(new RefreshingBearerAuthenticator(tokenRefresher));
        }
        OkHttpClient client = clientBuilder.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(configuration.baseUrl())
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build();
        return new MobileNetworkClient(
                retrofit,
                client,
                configuration);
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public static ProblemDetailsParser problemDetailsParser() {
        return new ProblemDetailsParser(objectMapper());
    }
}
