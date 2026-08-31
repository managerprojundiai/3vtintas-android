package br.com.tresvtintas.mobile.data.catalogadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.network.dto.CatalogAdministrationDtos;
import java.util.List;
import org.junit.Test;

public final class CatalogAdministrationDtoMapperTest {
    @Test
    public void mapsTheCompleteAuthorizedProductProjection() {
        Product product = CatalogAdministrationDtoMapper.product(productDto());

        assertEquals(
                "The optimistic revision must survive transport mapping.",
                7,
                product.revision());
        assertEquals(
                "The category must retain its opaque server identifier.",
                5L,
                product.category().orElseThrow().id());
        assertEquals(
                "Structured knowledge cannot be flattened or discarded.",
                List.of("parede", "alvenaria"),
                product.knowledge().intents());
        assertEquals(
                "Knowledge confidence must remain exact.",
                84,
                product.knowledge().confidence());
    }

    @Test
    public void preservesTheOpaquePaginationCursor() {
        Page page = CatalogAdministrationDtoMapper.page(
                new CatalogAdministrationDtos.Page(
                        List.of(productDto()),
                        "opaque_catalog_cursor"));

        assertEquals(
                "The server cursor must be returned unchanged.",
                "opaque_catalog_cursor",
                page.nextCursor().orElseThrow());
        assertEquals(
                "Every validated product must be mapped.",
                1,
                page.items().size());
    }

    @Test
    public void mapsReplayMetadataSeparatelyFromTheResponseBody() {
        var mutation = CatalogAdministrationDtoMapper.mutation(
                new CatalogAdministrationDtos.Mutation(91, 8, false),
                true);

        assertTrue(
                "A replay must remain observable to the UI.",
                mutation.replayed());
        assertEquals(
                "Replay cannot invent a new revision.",
                8,
                mutation.revision());
    }

    private static CatalogAdministrationDtos.Product productDto() {
        return new CatalogAdministrationDtos.Product(
                91,
                new CatalogAdministrationDtos.ProductCategory(5, "Tintas"),
                "Tinta Premium",
                "Tinta acrílica",
                "https://cdn.example.invalid/product.png",
                "https://catalog.example.invalid/product",
                "SKU-91",
                "lata",
                "18 L",
                "389.90",
                21,
                "3V Lab",
                true,
                7,
                new CatalogAdministrationDtos.Knowledge(
                        "manual",
                        "Ficha técnica",
                        List.of("premium"),
                        "Ambientes internos e externos",
                        "Rolo ou pincel",
                        "10%",
                        "250 m²",
                        List.of("parede", "alvenaria"),
                        "Alta cobertura",
                        "Qual é a área?",
                        "Evitar madeira",
                        "Posso calcular a quantidade.",
                        84),
                "2026-07-30T12:00:00Z",
                "2026-07-30T12:01:00Z");
    }
}
