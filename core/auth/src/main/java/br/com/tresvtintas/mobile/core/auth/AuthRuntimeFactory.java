package br.com.tresvtintas.mobile.core.auth;

import android.content.Context;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.DeviceInfo;
import br.com.tresvtintas.mobile.core.security.AccessTokenMemoryStore;
import br.com.tresvtintas.mobile.core.security.InstallationIdentityStore;
import br.com.tresvtintas.mobile.core.security.SessionStorageException;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Composition root for the authentication graph. No credential or client secret is configured here.
 */
public final class AuthRuntimeFactory {
    private AuthRuntimeFactory() {
        throw new AssertionError("No instances.");
    }

    public static AuthRuntime create(
            Context context,
            NetworkConfiguration configuration,
            String appVersion,
            Executor workerExecutor,
            Executor mainExecutor) throws AuthException {
        Context applicationContext = Objects.requireNonNull(
                context, "Application context is required.").getApplicationContext();
        Clock clock = Clock.systemUTC();
        String installationId;
        try {
            installationId = new InstallationIdentityStore(
                    applicationContext).getOrCreate();
        } catch (SessionStorageException exception) {
            throw new AuthException(
                    AuthFailureKind.STORAGE,
                    "The installation identity could not be loaded.",
                    exception);
        }
        DeviceInfo device = AndroidDeviceInfoFactory.create(installationId, appVersion);
        SessionCredentialStore credentialStore = new SessionCredentialStore(
                new AccessTokenMemoryStore(),
                new EncryptedSessionVault(applicationContext),
                clock);

        MobileApi publicApi = MobileApiFactory.create(configuration, credentialStore);
        MobileAuthRemote publicRemote = new RetrofitMobileAuthRemote(publicApi);
        AtomicReference<MobileAuthRemote> protectedRemote = new AtomicReference<>();
        AuthSessionEngine engine = new AuthSessionEngine(
                publicRemote,
                () -> Objects.requireNonNull(
                        protectedRemote.get(), "Protected auth remote is not initialized."),
                credentialStore,
                device,
                clock);
        MobileNetworkClient protectedClient = MobileApiFactory.createClient(
                configuration,
                credentialStore,
                engine);
        protectedRemote.set(new RetrofitMobileAuthRemote(
                protectedClient.api()));
        AuthController controller = new AuthController(
                engine,
                new CredentialManagerGoogleGateway(applicationContext, mainExecutor),
                workerExecutor,
                mainExecutor);
        return new AuthRuntime(controller, protectedClient);
    }
}
