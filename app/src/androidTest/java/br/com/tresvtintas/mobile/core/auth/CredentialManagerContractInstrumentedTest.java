package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.os.Bundle;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class CredentialManagerContractInstrumentedTest {
    private static final String CLIENT_ID = "client.apps.googleusercontent.com";
    private static final String ID_TOKEN =
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0."
                    + "eyJzdWIiOiJwZXJzb24tMSIsImVtYWlsIjoicGVyc29uQGV4YW1wbGUudGVzdCIs"
                    + "Im5hbWUiOiJUZXN0IFBlcnNvbiJ9."
                    + "c2lnbmF0dXJl";
    private static final String NONCE = "3vn1_" + "n".repeat(43);

    @Test
    public void productionFactoryConstructsBothOfficialGoogleCredentialFlows() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Credential Manager must load on the supported API.", CredentialManager.create(
                context));
        GoogleSignInPrompt automatic = new GoogleSignInPrompt(
                CLIENT_ID,
                NONCE,
                GoogleSignInMode.AUTHORIZED_WITH_FALLBACK);
        GoogleSignInPrompt explicit = new GoogleSignInPrompt(
                CLIENT_ID,
                NONCE,
                GoogleSignInMode.EXPLICIT_BUTTON);

        GetCredentialRequest bottomSheet = GoogleCredentialRequestFactory.account(
                automatic,
                true);
        GetCredentialRequest button = GoogleCredentialRequestFactory.explicit(explicit);

        assertEquals("Bottom sheet must contain one Google option.", 1,
                bottomSheet.getCredentialOptions().size());
        assertEquals("Explicit button must contain one Google option.", 1,
                button.getCredentialOptions().size());
    }

    @Test
    public void productionParserExtractsGoogleIdToken() throws AuthException {
        GoogleIdTokenCredential credential = new GoogleIdTokenCredential.Builder()
                .setId("person@example.test")
                .setIdToken(ID_TOKEN)
                .build();

        String parsed = GoogleCredentialResponseParser.parse(
                new GetCredentialResponse(credential));

        assertEquals("Parser must return the provider token unchanged.", ID_TOKEN, parsed);
    }

    @Test
    public void productionParserRejectsUnknownCredentialType() {
        GetCredentialResponse response = new GetCredentialResponse(
                new CustomCredential("unsupported.credential", new Bundle()));

        AuthException failure = assertThrows(
                "Unknown credential types must fail closed.",
                AuthException.class,
                () -> GoogleCredentialResponseParser.parse(response));

        assertEquals("Unknown credential must map to protocol failure.",
                AuthFailureKind.PROTOCOL, failure.kind());
    }
}
