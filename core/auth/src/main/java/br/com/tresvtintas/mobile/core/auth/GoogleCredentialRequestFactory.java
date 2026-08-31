package br.com.tresvtintas.mobile.core.auth;

import androidx.credentials.GetCredentialRequest;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;

final class GoogleCredentialRequestFactory {
    private GoogleCredentialRequestFactory() {
        throw new AssertionError("No instances.");
    }

    static GetCredentialRequest account(
            GoogleSignInPrompt prompt,
            boolean authorizedOnly) {
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(authorizedOnly)
                .setAutoSelectEnabled(authorizedOnly)
                .setServerClientId(prompt.serverClientId())
                .setNonce(prompt.nonce())
                .build();
        return new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();
    }

    static GetCredentialRequest explicit(GoogleSignInPrompt prompt) {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                prompt.serverClientId())
                .setNonce(prompt.nonce())
                .build();
        return new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();
    }
}
