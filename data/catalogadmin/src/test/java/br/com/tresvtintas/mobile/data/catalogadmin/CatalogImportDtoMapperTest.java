package br.com.tresvtintas.mobile.data.catalogadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Action;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.RowStatus;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Status;
import br.com.tresvtintas.mobile.core.network.dto.CatalogImportDtos;
import java.util.List;
import org.junit.Test;

public final class CatalogImportDtoMapperTest {
    @Test
    public void mapsCompletePreviewWithoutInventingAuthority() {
        var batch = CatalogImportDtoMapper.batch(view());

        assertEquals(
                "The optimistic revision must survive transport mapping.",
                3,
                batch.revision());
        assertEquals(
                "The preview digest must remain exact.",
                "c".repeat(64),
                batch.previewDigest().orElseThrow());
        assertEquals(
                "The row action must remain server-authored.",
                Action.UPDATE,
                batch.rows().get(0).action());
        assertEquals(
                "The validation result must remain typed.",
                RowStatus.READY,
                batch.rows().get(0).status());
        assertEquals(
                "The decimal price must not be converted through floating point.",
                "389.90",
                batch.rows().get(0).preview().price().orElseThrow());
        assertTrue(
                "A complete preview must remain confirmable.",
                batch.confirmable());
    }

    @Test
    public void mapsReplaySeparatelyFromMutationBody() {
        var mutation = CatalogImportDtoMapper.mutation(
                new CatalogImportDtos.Mutation(
                        "10000000-0000-4000-8000-000000000011",
                        4,
                        "queued"),
                true);

        assertEquals(
                "The queued server state must remain typed.",
                Status.QUEUED,
                mutation.status());
        assertTrue(
                "Transport replay metadata must reach the domain.",
                mutation.replayed());
    }

    private static CatalogImportDtos.View view() {
        String now = "2026-07-31T12:00:00Z";
        return new CatalogImportDtos.View(
                "10000000-0000-4000-8000-000000000011",
                "catalog.xlsx",
                "xlsx",
                "a".repeat(64),
                "preview_ready",
                3,
                "c".repeat(64),
                1,
                1,
                0,
                0,
                0,
                null,
                "2026-08-01T12:00:00Z",
                null,
                null,
                now,
                now,
                List.of(new CatalogImportDtos.Row(
                        2,
                        "update",
                        "ready",
                        new CatalogImportDtos.Preview(
                                "Tinta Premium",
                                "SKU-91",
                                "389.90",
                                21,
                                "Tintas"),
                        null)),
                "1");
    }
}
