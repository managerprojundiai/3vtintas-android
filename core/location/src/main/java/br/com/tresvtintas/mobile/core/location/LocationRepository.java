package br.com.tresvtintas.mobile.core.location;

import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface LocationRepository {
    LocationDtos.PolicyResponse policy() throws IOException;

    LocationDtos.ConsentResponse consent(
            long policyRevision,
            String disclosureVersion,
            boolean accepted) throws IOException;

    LocationDtos.HeartbeatResponse heartbeat(
            LocationDtos.DeviceReport report) throws IOException;

    LocationDtos.BatchResponse send(
            String batchKey,
            List<LocationDtos.Point> points,
            LocationDtos.DeviceReport report) throws IOException;

    LocationDtos.HistoryResponse history(
            Instant from,
            Instant to,
            int limit) throws IOException;
}
