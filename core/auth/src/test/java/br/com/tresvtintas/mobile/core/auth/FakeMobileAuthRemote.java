package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginResponse;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.RefreshResponse;
import java.util.concurrent.atomic.AtomicInteger;

final class FakeMobileAuthRemote implements MobileAuthRemote {
    final AtomicInteger refreshCalls = new AtomicInteger();
    AuthChallengeResponse challenge = AuthTestFixtures.challenge();
    GoogleLoginResponse loginResponse = new GoogleLoginResponse(
            AuthTestFixtures.user(),
            AuthTestFixtures.credentials(
                    AuthTestFixtures.ACCESS_TOKEN_A,
                    AuthTestFixtures.REFRESH_TOKEN_A));
    RefreshResponse refreshResponse = new RefreshResponse(
            AuthTestFixtures.credentials(
                    AuthTestFixtures.ACCESS_TOKEN_B,
                    AuthTestFixtures.REFRESH_TOKEN_B));
    MeResponse meResponse = AuthTestFixtures.me();
    AuthException refreshFailure;
    AuthException logoutFailure;
    GoogleLoginRequest capturedLogin;
    boolean logoutCalled;

    @Override
    public AuthChallengeResponse createChallenge(String installationId) {
        return challenge;
    }

    @Override
    public GoogleLoginResponse login(GoogleLoginRequest request) {
        capturedLogin = request;
        return loginResponse;
    }

    @Override
    public RefreshResponse refresh(String refreshToken) throws AuthException {
        refreshCalls.incrementAndGet();
        if (refreshFailure != null) {
            throw refreshFailure;
        }
        return refreshResponse;
    }

    @Override
    public MeResponse me() {
        return meResponse;
    }

    @Override
    public void logout() throws AuthException {
        logoutCalled = true;
        if (logoutFailure != null) {
            throw logoutFailure;
        }
    }
}
