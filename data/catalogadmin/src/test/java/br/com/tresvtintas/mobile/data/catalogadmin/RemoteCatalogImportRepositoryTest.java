package br.com.tresvtintas.mobile.data.catalogadmin;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationException;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Status;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteCatalogImportRepositoryTest {
    private static final String IMPORT_ID =
            "10000000-0000-4000-8000-000000000011";
    private static final String IDEMPOTENCY_KEY =
            "20000000-0000-4000-8000-000000000011";
    private static final String FILE_DIGEST = "a".repeat(64);
    private static final String PREVIEW_DIGEST = "c".repeat(64);
    private MockWebServer server;
    private RemoteCatalogImportRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteCatalogImportRepository(
                new CatalogAdministrationAccountScope(41, "d".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.48.0-catalog-import",
                                56,
                                true),
                        () -> Optional.of("b".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void uploadsExactBinaryWithDigestAndIdempotencyHeaders()
            throws Exception {
        byte[] bytes = "sku,name\nSKU-1,Tinta".getBytes(StandardCharsets.UTF_8);
        server.enqueue(json(202, mutation("uploaded", 1))
                .setHeader("X-Idempotency-Replayed", "false"));

        var result = repository.upload(
                new SourceFile(
                        "Catálogo São José.csv",
                        "text/csv",
                        FILE_DIGEST,
                        bytes),
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Upload must use the dedicated protected endpoint.",
                "/api/mobile/v1/catalog-administration/imports",
                request.getPath());
        assertEquals(
                "The file name must be bound outside parser-controlled data.",
                "Cat%C3%A1logo%20S%C3%A3o%20Jos%C3%A9.csv",
                request.getHeader("X-3V-File-Name"));
        assertEquals(
                "The device digest must be sent unchanged.",
                FILE_DIGEST,
                request.getHeader("X-Content-SHA256"));
        assertEquals(
                "Retry identity must remain stable for the request.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertEquals(
                "The protected bearer stack must be used.",
                "Bearer " + "b".repeat(80),
                request.getHeader("Authorization"));
        assertArrayEquals(
                "No multipart or text conversion may alter the catalog bytes.",
                bytes,
                request.getBody().readByteArray());
        assertEquals(
                "The accepted upload must remain in server state.",
                Status.UPLOADED,
                result.status());
    }

    @Test
    public void confirmsTheExactPreviewRevisionAndDigest()
            throws Exception {
        server.enqueue(json(202, mutation("queued", 4))
                .setHeader("X-Idempotency-Replayed", "true"));

        var result = repository.confirm(
                IMPORT_ID,
                3,
                PREVIEW_DIGEST,
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Confirmation must address only the opaque import ID.",
                "/api/mobile/v1/catalog-administration/imports/"
                        + IMPORT_ID
                        + "/confirmation",
                request.getPath());
        assertTrue(
                "The displayed revision must be bound in the command.",
                body.contains("\"expectedRevision\":3"));
        assertTrue(
                "The displayed digest must be bound in the command.",
                body.contains("\"previewDigest\":\""
                        + PREVIEW_DIGEST
                        + "\""));
        assertTrue(
                "The destructive intent must be explicit.",
                body.contains("\"confirmed\":true"));
        assertTrue(
                "Server replay metadata must remain observable.",
                result.replayed());
    }

    @Test
    public void mapsOversizedPayloadAndClosedScopeWithoutLeakingRequests()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(413)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {
                          "type":"about:blank",
                          "title":"Payload too large",
                          "status":413,
                          "code":"PAYLOAD_TOO_LARGE",
                          "detail":"File is too large.",
                          "instance":"/api/mobile/v1/catalog-administration/imports",
                          "requestId":"30000000-0000-4000-8000-000000000011"
                        }
                        """));
        SourceFile source = new SourceFile(
                "catalog.csv",
                "text/csv",
                FILE_DIGEST,
                "sku,name".getBytes(StandardCharsets.UTF_8));

        CatalogAdministrationException oversized = assertThrows(
                CatalogAdministrationException.class,
                () -> repository.upload(source, IDEMPOTENCY_KEY));
        server.takeRequest();
        repository.close();
        CatalogAdministrationException closed = assertThrows(
                CatalogAdministrationException.class,
                () -> repository.upload(source, IDEMPOTENCY_KEY));

        assertEquals(
                "HTTP 413 must retain its typed user-facing failure.",
                CatalogAdministrationFailureKind.PAYLOAD_TOO_LARGE,
                oversized.kind());
        assertEquals(
                "A closed account scope must fail closed.",
                CatalogAdministrationFailureKind.ACCESS_REVOKED,
                closed.kind());
        assertEquals(
                "Closed scopes cannot emit another request.",
                1,
                server.getRequestCount());
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String mutation(String status, int revision) {
        return "{\"importId\":\""
                + IMPORT_ID
                + "\",\"revision\":"
                + revision
                + ",\"status\":\""
                + status
                + "\"}";
    }
}
