package br.com.tresvtintas.mobile.feature.catalog;

import br.com.tresvtintas.mobile.core.catalog.CatalogController;
import java.util.Optional;

/**
 * Application-level bridge. A catalog controller exists only while the authenticated bootstrap
 * still grants the implemented catalog capability.
 */
@FunctionalInterface
public interface CatalogRuntimeProvider {
    Optional<CatalogController> catalogController();
}
