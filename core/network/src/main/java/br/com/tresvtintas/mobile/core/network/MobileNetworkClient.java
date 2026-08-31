package br.com.tresvtintas.mobile.core.network;

import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

/**
 * Hardened mobile transport shared by Retrofit and authenticated streaming requests.
 */
public final class MobileNetworkClient {
    private static final Duration STREAM_READ_TIMEOUT =
            Duration.ofSeconds(30);
    private static final Pattern RELATIVE_STREAM_PATH = Pattern.compile(
            "^[a-z0-9][a-z0-9_/-]{0,159}$");
    private static final Pattern EVENT_CURSOR = Pattern.compile(
            "^\\d{1,16}$");
    private final Retrofit retrofit;
    private final OkHttpClient streamClient;
    private final HttpUrl baseUrl;

    MobileNetworkClient(
            Retrofit retrofit,
            OkHttpClient client,
            NetworkConfiguration configuration) {
        this.retrofit = Objects.requireNonNull(
                retrofit,
                "Mobile Retrofit client is required.");
        OkHttpClient requiredClient = Objects.requireNonNull(
                client,
                "Mobile HTTP client is required.");
        baseUrl = HttpUrl.get(URI.create(Objects.requireNonNull(
                configuration,
                "Network configuration is required.").baseUrl()));
        streamClient = requiredClient.newBuilder()
                .callTimeout(Duration.ZERO)
                .readTimeout(STREAM_READ_TIMEOUT)
                .build();
    }

    public MobileApi api() {
        return retrofit.create(MobileApi.class);
    }

    public Call newAuthenticatedEventStreamCall(
            String relativePath,
            Optional<String> lastEventId) {
        String path = Objects.requireNonNull(
                relativePath,
                "Event stream path is required.");
        Optional<String> cursor = Objects.requireNonNull(
                lastEventId,
                "Last event ID is required.");
        if (!RELATIVE_STREAM_PATH.matcher(path).matches()
                || path.contains("//")
                || path.contains("..")) {
            throw new IllegalArgumentException(
                    "Event stream path is invalid.");
        }
        if (cursor.isPresent()
                && !EVENT_CURSOR.matcher(cursor.orElseThrow()).matches()) {
            throw new IllegalArgumentException(
                    "Last event ID is invalid.");
        }
        HttpUrl url = baseUrl.resolve(path);
        if (url == null
                || !url.toString().startsWith(baseUrl.toString())) {
            throw new IllegalArgumentException(
                    "Event stream URL is invalid.");
        }
        Request.Builder request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "text/event-stream")
                .tag(
                        AuthenticatedRequestTag.class,
                        AuthenticatedRequestTag.INSTANCE);
        cursor.ifPresent(value ->
                request.header("Last-Event-ID", value));
        return streamClient.newCall(request.build());
    }
}
