package br.com.tresvtintas.mobile.data.agent;

import br.com.tresvtintas.mobile.core.agent.AgentConversation;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecisionResult;
import br.com.tresvtintas.mobile.core.agent.AgentConversationPage;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentMessagePage;
import br.com.tresvtintas.mobile.core.agent.AgentRepository;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpChallenge;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpGrant;
import br.com.tresvtintas.mobile.core.agent.AgentTurn;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AgentCancelTurnRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDecisionRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentCreateConversationRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentCreateTurnRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpChallengeRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpGrantResponse;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpVerifyRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentTurnDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteAgentRepository
        implements AgentRepository, AutoCloseable {
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private final AgentAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteAgentRepository(
            AgentAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Agent account scope is required.");
        this.api = Objects.requireNonNull(
                api,
                "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public AgentAccountScope scope() {
        return scope;
    }

    @Override
    public AgentConversationPage conversations(
            Optional<String> cursor,
            int limit) throws AgentException {
        requireActive();
        Objects.requireNonNull(cursor, "Agent cursor is required.");
        requireLimit(limit);
        try {
            Response<AgentConversationPageDto> response = execute(
                    api.agentConversations(
                            cursor.orElse(null),
                            limit,
                            "active"));
            requireStatus(response, HTTP_OK);
            return AgentDtoMapper.conversations(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent conversation page is invalid.",
                    failure);
        }
    }

    @Override
    public AgentConversation createConversation(
            String title,
            String idempotencyKey) throws AgentException {
        requireActive();
        try {
            Response<AgentConversationDto> response = execute(
                    api.createAgentConversation(
                            idempotencyKey,
                            new AgentCreateConversationRequest(title)));
            requireStatus(response, HTTP_CREATED);
            requireReplayHeader(response);
            return AgentDtoMapper.conversation(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Created agent conversation is invalid.",
                    failure);
        }
    }

    @Override
    public AgentConversation conversation(
            String conversationId) throws AgentException {
        requireActive();
        try {
            Response<AgentConversationDto> response = execute(
                    api.agentConversation(conversationId));
            requireStatus(response, HTTP_OK);
            return AgentDtoMapper.conversation(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent conversation response is invalid.",
                    failure);
        }
    }

    @Override
    public AgentMessagePage messages(
            String conversationId,
            Optional<String> cursor,
            int limit) throws AgentException {
        requireActive();
        Objects.requireNonNull(cursor, "Agent message cursor is required.");
        requireLimit(limit);
        try {
            Response<AgentMessagePageDto> response = execute(
                    api.agentMessages(
                            conversationId,
                            cursor.orElse(null),
                            limit));
            requireStatus(response, HTTP_OK);
            return AgentDtoMapper.messages(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent message page is invalid.",
                    failure);
        }
    }

    @Override
    public AgentTurn enqueue(
            String conversationId,
            String message,
            String idempotencyKey) throws AgentException {
        requireActive();
        try {
            Response<AgentTurnDto> response = execute(
                    api.enqueueAgentTurn(
                            conversationId,
                            idempotencyKey,
                            new AgentCreateTurnRequest(message)));
            requireStatus(response, HTTP_ACCEPTED);
            requireReplayHeader(response);
            return AgentDtoMapper.turn(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol("Agent turn response is invalid.", failure);
        }
    }

    @Override
    public AgentTurn turn(String turnId) throws AgentException {
        requireActive();
        try {
            Response<AgentTurnDto> response = execute(
                    api.agentTurn(turnId));
            requireStatus(response, HTTP_OK);
            return AgentDtoMapper.turn(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol("Agent turn response is invalid.", failure);
        }
    }

    @Override
    public AgentTurn cancel(
            String turnId,
            String idempotencyKey) throws AgentException {
        requireActive();
        try {
            Response<AgentTurnDto> response = execute(
                    api.cancelAgentTurn(
                            turnId,
                            idempotencyKey,
                            AgentCancelTurnRequest.confirmed()));
            requireStatus(response, HTTP_OK);
            requireReplayHeader(response);
            return AgentDtoMapper.turn(body(response));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Cancelled agent turn response is invalid.",
                    failure);
        }
    }

    @Override
    public AgentActionDecisionResult decideAction(
            String actionId,
            AgentActionDecision decision,
            String idempotencyKey) throws AgentException {
        return decideAction(
                actionId,
                decision,
                idempotencyKey,
                Optional.empty());
    }

    @Override
    public AgentActionDecisionResult decideAction(
            String actionId,
            AgentActionDecision decision,
            String idempotencyKey,
            Optional<String> stepUpToken) throws AgentException {
        requireActive();
        Objects.requireNonNull(
                decision,
                "Agent action decision is required.");
        Objects.requireNonNull(
                stepUpToken,
                "Agent step-up token is required.");
        try {
            Response<AgentActionDto> response = execute(
                    api.decideAgentAction(
                            actionId,
                            idempotencyKey,
                            new AgentActionDecisionRequest(
                                    decision.wireValue(),
                                    decision.confirmation(),
                                    stepUpToken.orElse(null))));
            requireStatus(response, HTTP_OK);
            boolean replayed = replayHeader(response);
            return new AgentActionDecisionResult(
                    AgentDtoMapper.action(body(response)),
                    replayed);
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent action decision response is invalid.",
                    failure);
        }
    }

    @Override
    public AgentStepUpChallenge requestStepUp(
            String actionId) throws AgentException {
        requireActive();
        try {
            Response<AgentStepUpChallengeResponse> response = execute(
                    api.requestAgentActionStepUp(
                            actionId,
                            AgentStepUpChallengeRequest.confirmed()));
            requireStatus(response, HTTP_CREATED);
            AgentStepUpChallengeResponse value = body(response);
            return new AgentStepUpChallenge(
                    value.challengeId(),
                    value.nonce(),
                    value.googleServerClientId(),
                    java.time.Instant.parse(value.expiresAt()));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent step-up challenge is invalid.",
                    failure);
        }
    }

    @Override
    public AgentStepUpGrant verifyStepUp(
            String actionId,
            String challengeId,
            String credential) throws AgentException {
        requireActive();
        try {
            Response<AgentStepUpGrantResponse> response = execute(
                    api.verifyAgentActionStepUp(
                            actionId,
                            new AgentStepUpVerifyRequest(
                                    challengeId,
                                    credential)));
            requireStatus(response, HTTP_OK);
            AgentStepUpGrantResponse value = body(response);
            return new AgentStepUpGrant(
                    value.stepUpToken(),
                    java.time.Instant.parse(value.expiresAt()));
        } catch (IllegalArgumentException failure) {
            throw protocol(
                    "Agent step-up grant is invalid.",
                    failure);
        }
    }

    @Override
    public void close() {
        active.set(false);
    }

    private void requireActive() throws AgentException {
        if (!active.get()) {
            throw new AgentException(
                    AgentFailureKind.ACCESS_REVOKED,
                    "Agent account scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws AgentException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException failure) {
            throw new AgentException(
                    AgentFailureKind.AUTH_REJECTED,
                    "Protected agent session is unavailable.",
                    failure);
        } catch (IOException failure) {
            throw new AgentException(
                    protocolFailure(failure)
                            ? AgentFailureKind.PROTOCOL
                            : AgentFailureKind.NETWORK,
                    "Agent response could not be read.",
                    failure);
        }
    }

    private AgentException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AgentException(
                    map(details.code()),
                    "Agent request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new AgentException(
                status(response.code()),
                "Agent error response is invalid.");
    }

    private static AgentFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    AgentFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                    AgentFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> AgentFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    AgentFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    AgentFailureKind.IDEMPOTENCY_KEY_REUSED;
            case NOT_FOUND -> AgentFailureKind.NOT_FOUND;
            case INVALID_JSON, INVALID_REQUEST ->
                    AgentFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> AgentFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> AgentFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    AgentFailureKind.SERVICE_UNAVAILABLE;
            default -> AgentFailureKind.PROTOCOL;
        };
    }

    private static AgentFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AgentFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AgentFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AgentFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return AgentFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return AgentFailureKind.CONFLICT;
        }
        if (code == HTTP_UNPROCESSABLE) {
            return AgentFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AgentFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AgentFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AgentFailureKind.SERVICE_UNAVAILABLE;
        }
        return AgentFailureKind.PROTOCOL;
    }

    private static <T> T body(Response<T> response)
            throws AgentException {
        if (response.body() == null) {
            throw new AgentException(
                    AgentFailureKind.PROTOCOL,
                    "Agent response has no body.");
        }
        return response.body();
    }

    private static void requireStatus(
            Response<?> response,
            int expected) throws AgentException {
        if (response.code() != expected) {
            throw new AgentException(
                    AgentFailureKind.PROTOCOL,
                    "Agent success status is inconsistent.");
        }
    }

    private static void requireReplayHeader(Response<?> response)
            throws AgentException {
        replayHeader(response);
    }

    private static boolean replayHeader(Response<?> response)
            throws AgentException {
        String value = response.headers().get(
                "X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new AgentException(
                    AgentFailureKind.PROTOCOL,
                    "Agent idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static void requireLimit(int value) throws AgentException {
        if (value < 1 || value > 100) {
            throw new AgentException(
                    AgentFailureKind.INVALID_REQUEST,
                    "Agent page size is invalid.");
        }
    }

    private static AgentException protocol(
            String message,
            IllegalArgumentException failure) {
        return new AgentException(
                AgentFailureKind.PROTOCOL,
                message,
                failure);
    }

    private static boolean protocolFailure(IOException failure) {
        Throwable cursor = failure;
        while (cursor != null) {
            if (cursor instanceof JacksonException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
