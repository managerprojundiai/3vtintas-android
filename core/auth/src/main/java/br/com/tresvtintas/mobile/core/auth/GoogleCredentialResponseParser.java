package br.com.tresvtintas.mobile.core.auth;

import androidx.credentials.Credential;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialResponse;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

final class GoogleCredentialResponseParser {
    private static final int MAXIMUM_ID_TOKEN_LENGTH = 8192;

    private GoogleCredentialResponseParser() {
        throw new AssertionError("No instances.");
    }

    static String parse(GetCredentialResponse response) throws AuthException {
        Credential credential = response.getCredential();
        if (!(credential instanceof CustomCredential custom)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                        custom.getType())) {
            throw new AuthException(
                    AuthFailureKind.PROTOCOL,
                    "Credential Manager returned an unsupported credential.");
        }
        try {
            String idToken = GoogleIdTokenCredential.createFrom(
                    custom.getData()).getIdToken();
            if (idToken.length() > MAXIMUM_ID_TOKEN_LENGTH) {
                throw new GoogleIdTokenParsingException();
            }
            return idToken;
        } catch (GoogleIdTokenParsingException | IllegalArgumentException exception) {
            throw new AuthException(
                    AuthFailureKind.PROTOCOL,
                    "Credential Manager returned an invalid Google ID token.",
                    exception);
        }
    }
}
