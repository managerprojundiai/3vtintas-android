package br.com.tresvtintas.mobile.data.notifications;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.NotificationPreferencesDto;
import br.com.tresvtintas.mobile.core.network.dto.NotificationPreferencesUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.PushRegistrationRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationException;
import br.com.tresvtintas.mobile.core.notifications.NotificationFailureKind;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationPreferences;
import br.com.tresvtintas.mobile.core.notifications.NotificationRepository;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Response;

public final class RemoteNotificationRepository
        implements NotificationRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final NotificationAccountScope accountScope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteNotificationRepository(
            NotificationAccountScope accountScope,
            MobileApi api) {
        this.accountScope = Objects.requireNonNull(
                accountScope,
                "Notification account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public NotificationAccountScope accountScope() {
        return accountScope;
    }

    @Override
    public NotificationPreferences load() throws NotificationException {
        requireActive();
        try {
            Response<NotificationPreferencesDto> response =
                    api.notificationPreferences().execute();
            return preferences(response);
        } catch (AuthenticationRequiredException exception) {
            throw authenticationFailure(exception);
        } catch (IOException exception) {
            throw ioFailure(exception);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    @Override
    public NotificationPreferences update(
            NotificationPermissionState permissionState,
            boolean operationalEnabled,
            Map<NotificationCategory, Boolean> categories,
            int expectedRevision) throws NotificationException {
        requireActive();
        try {
            NotificationPreferencesUpdateRequest request =
                    new NotificationPreferencesUpdateRequest(
                            permissionState.wireValue(),
                            operationalEnabled,
                            NotificationDtoMapper.categories(categories),
                            expectedRevision);
            Response<NotificationPreferencesDto> response =
                    api.updateNotificationPreferences(request).execute();
            return preferences(response);
        } catch (AuthenticationRequiredException exception) {
            throw authenticationFailure(exception);
        } catch (IOException exception) {
            throw ioFailure(exception);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    @Override
    public void register(String firebaseInstallationId)
            throws NotificationException {
        requireActive();
        try {
            Response<?> response = api.registerPush(
                    new PushRegistrationRequest(firebaseInstallationId)).execute();
            requireSuccess(response);
        } catch (AuthenticationRequiredException exception) {
            throw authenticationFailure(exception);
        } catch (IOException exception) {
            throw ioFailure(exception);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    @Override
    public void unregister() throws NotificationException {
        requireActive();
        try {
            requireSuccess(api.unregisterPush().execute());
        } catch (AuthenticationRequiredException exception) {
            throw authenticationFailure(exception);
        } catch (IOException exception) {
            throw ioFailure(exception);
        } catch (IllegalArgumentException exception) {
            throw protocolFailure(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private NotificationPreferences preferences(
            Response<NotificationPreferencesDto> response)
            throws NotificationException {
        requireSuccess(response);
        if (response.body() == null) {
            throw new NotificationException(
                    NotificationFailureKind.PROTOCOL,
                    "Notification preferences response did not contain a body.");
        }
        return NotificationDtoMapper.preferences(response.body());
    }

    private void requireSuccess(Response<?> response)
            throws NotificationException {
        if (!response.isSuccessful()) {
            throw failure(response);
        }
        if (response.body() == null) {
            throw new NotificationException(
                    NotificationFailureKind.PROTOCOL,
                    "Notification response did not contain a body.");
        }
    }

    private void requireActive() throws NotificationException {
        if (!active.get()) {
            throw new NotificationException(
                    NotificationFailureKind.ACCESS_REVOKED,
                    "Notification account scope is no longer active.");
        }
    }

    private NotificationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new NotificationException(
                    map(details.code()),
                    "The notification request was rejected.",
                    details.requestId(),
                    null);
        }
        return new NotificationException(
                status(response.code()),
                "The notification service returned an invalid error response.");
    }

    private static NotificationFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                NotificationFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                NotificationFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> NotificationFailureKind.ACCESS_REVOKED;
            case RESOURCE_CONFLICT -> NotificationFailureKind.CONFLICT;
            case INVALID_JSON, INVALID_REQUEST ->
                NotificationFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> NotificationFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                NotificationFailureKind.SERVICE_UNAVAILABLE;
            default -> NotificationFailureKind.PROTOCOL;
        };
    }

    private static NotificationFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return NotificationFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return NotificationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return NotificationFailureKind.ACCESS_REVOKED;
        }
        if (code == HTTP_CONFLICT) {
            return NotificationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return NotificationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return NotificationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return NotificationFailureKind.SERVICE_UNAVAILABLE;
        }
        return NotificationFailureKind.PROTOCOL;
    }

    private static NotificationException authenticationFailure(
            AuthenticationRequiredException exception) {
        return new NotificationException(
                NotificationFailureKind.AUTH_REJECTED,
                "The protected notification session is unavailable.",
                exception);
    }

    private static NotificationException ioFailure(IOException exception) {
        return new NotificationException(
                isProtocolFailure(exception)
                        ? NotificationFailureKind.PROTOCOL
                        : NotificationFailureKind.NETWORK,
                "The notification response could not be read.",
                exception);
    }

    private static NotificationException protocolFailure(
            IllegalArgumentException exception) {
        return new NotificationException(
                NotificationFailureKind.PROTOCOL,
                "The notification response was invalid.",
                exception);
    }

    private static boolean isProtocolFailure(IOException exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof JacksonException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
