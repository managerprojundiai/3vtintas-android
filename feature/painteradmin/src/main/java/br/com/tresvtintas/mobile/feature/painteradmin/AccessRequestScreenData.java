package br.com.tresvtintas.mobile.feature.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import java.util.Objects;

record AccessRequestScreenData(
        AccessRequestDetail detail,
        Options options) {
    AccessRequestScreenData {
        Objects.requireNonNull(detail, "Access request detail is required.");
        Objects.requireNonNull(options, "Access request options are required.");
    }
}
