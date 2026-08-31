package br.com.tresvtintas.mobile.feature.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import java.util.Objects;

record PainterDetailScreenData(PainterDetail detail, Options options) {
    PainterDetailScreenData {
        Objects.requireNonNull(detail, "Painter detail is required.");
        Objects.requireNonNull(options, "Painter options are required.");
    }
}
