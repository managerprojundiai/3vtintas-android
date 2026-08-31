package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginResponse;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.RefreshResponse;

interface MobileAuthRemote {
    AuthChallengeResponse createChallenge(String installationId) throws AuthException;

    GoogleLoginResponse login(GoogleLoginRequest request) throws AuthException;

    RefreshResponse refresh(String refreshToken) throws AuthException;

    MeResponse me() throws AuthException;

    void logout() throws AuthException;
}
