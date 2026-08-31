package br.com.tresvtintas.mobile.core.network.problem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Response;

public final class ProblemDetailsParserTest {
    @Test
    public void parsesBoundedProblemJson() {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String json = "{"
                + "\"type\":\"https://www.3vtintas.com.br/problems/rate-limited\","
                + "\"title\":\"Limite excedido\","
                + "\"status\":429,"
                + "\"detail\":\"Tente novamente mais tarde.\","
                + "\"instance\":\"urn:3v:request:" + requestId + "\","
                + "\"code\":\"RATE_LIMITED\","
                + "\"requestId\":\"" + requestId + "\""
                + "}";
        ProblemDetailsParser parser = new ProblemDetailsParser(MobileApiFactory.objectMapper());
        try (ResponseBody body = ResponseBody.create(
                json, MediaType.get("application/problem+json"))) {
            Response<Object> response = Response.error(429, body);
            Optional<ProblemDetails> result = parser.parse(response);

            assertTrue("Valid Problem Details must parse.", result.isPresent());
            assertParsedProblem(result.orElseThrow(), requestId);
        }
    }

    @Test
    public void rejectsWrongMediaType() {
        try (ResponseBody body = ResponseBody.create(
                "{}", MediaType.get("application/json"))) {
            Response<Object> response = Response.error(400, body);
            Optional<ProblemDetails> result = new ProblemDetailsParser(
                    MobileApiFactory.objectMapper()).parse(response);

            assertTrue("Non-problem media type must be ignored.", result.isEmpty());
        }
    }

    private static void assertParsedProblem(ProblemDetails problem, String requestId) {
        assertEquals(
                "Stable server code must be preserved.",
                MobileProblemCode.RATE_LIMITED,
                problem.code());
        assertEquals(
                "Correlation request ID must be preserved.",
                requestId,
                problem.requestId());
    }
}
