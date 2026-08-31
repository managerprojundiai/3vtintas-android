package br.com.tresvtintas.mobile.data.location;

import br.com.tresvtintas.mobile.core.location.LocationRepository;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteLocationRepository implements LocationRepository {
    private static final long MINIMUM_IDENTIFIER = 1L;
    private final long organizationId;
    private final MobileApi api;

    public RemoteLocationRepository(
            long organizationId,
            MobileApi api) {
        if (organizationId < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Organization is required.");
        }
        this.organizationId = organizationId;
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
    }

    @Override
    public LocationDtos.PolicyResponse policy() throws IOException {
        return execute(api.locationPolicy(organizationId));
    }

    @Override
    public LocationDtos.ConsentResponse consent(
            long policyRevision,
            String disclosureVersion,
            boolean accepted) throws IOException {
        return execute(api.locationConsent(new LocationDtos.ConsentRequest(
                organizationId,
                policyRevision,
                disclosureVersion,
                accepted)));
    }

    @Override
    public LocationDtos.HeartbeatResponse heartbeat(
            LocationDtos.DeviceReport report) throws IOException {
        return execute(api.locationHeartbeat(new LocationDtos.HeartbeatRequest(
                organizationId,
                report)));
    }

    @Override
    public LocationDtos.BatchResponse send(
            String batchKey,
            List<LocationDtos.Point> points,
            LocationDtos.DeviceReport report) throws IOException {
        return execute(api.locationBatch(new LocationDtos.BatchRequest(
                organizationId,
                batchKey,
                points,
                report)));
    }

    @Override
    public LocationDtos.HistoryResponse history(
            Instant from,
            Instant to,
            int limit) throws IOException {
        return execute(api.locationHistory(
                organizationId,
                from.toString(),
                to.toString(),
                limit));
    }

    private static <T> T execute(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        T body = response.body();
        if (!response.isSuccessful() || body == null) {
            throw new IOException("Location API rejected the request: " + response.code());
        }
        return body;
    }
}
