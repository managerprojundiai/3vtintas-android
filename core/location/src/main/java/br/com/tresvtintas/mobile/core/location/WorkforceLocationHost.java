package br.com.tresvtintas.mobile.core.location;

import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.util.Optional;
import java.util.OptionalLong;

/** Runtime bridge kept free of Android location APIs so the public APK remains location-free. */
public interface WorkforceLocationHost {
    Optional<MobileApi> workforceLocationApi();

    OptionalLong workforceLocationOrganizationId();

    String workforceLocationAppVersion();

    boolean isWorkforceLocationBuild();
}
