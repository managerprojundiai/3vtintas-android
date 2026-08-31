package br.com.tresvtintas.mobile.app;

import android.app.Application;
import android.os.Build;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.auth.AuthController;
import br.com.tresvtintas.mobile.core.auth.AuthException;
import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.AuthRuntime;
import br.com.tresvtintas.mobile.core.auth.AuthRuntimeFactory;
import br.com.tresvtintas.mobile.core.auth.AuthState;
import br.com.tresvtintas.mobile.core.auth.AuthStateListener;
import br.com.tresvtintas.mobile.core.auth.AuthenticatedSession;
import br.com.tresvtintas.mobile.core.auth.CredentialManagerGoogleIdTokenRequester;
import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.audit.AgentReplayRepository;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapController;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapRuntimeFactory;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapState;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapStateListener;
import br.com.tresvtintas.mobile.core.bootstrap.ClientCompatibility;
import br.com.tresvtintas.mobile.core.bootstrap.ExpectedBootstrapIdentity;
import br.com.tresvtintas.mobile.core.catalog.CatalogController;
import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.customer.CustomerRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.location.WorkforceLocationHost;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.notifications.FirebaseClientConfiguration;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationController;
import br.com.tresvtintas.mobile.data.catalog.CachedCatalogRepository;
import br.com.tresvtintas.mobile.data.accountaccess.ManagedAccountAccessScope;
import br.com.tresvtintas.mobile.data.accountaccess.RemoteManagedAccountAccessReader;
import br.com.tresvtintas.mobile.data.accountaccess.RemoteManagedAccountRevocationRepository;
import br.com.tresvtintas.mobile.data.appointment.AppointmentAccountScope;
import br.com.tresvtintas.mobile.data.appointment.RemoteAppointmentRepository;
import br.com.tresvtintas.mobile.data.audit.AuditAccountScope;
import br.com.tresvtintas.mobile.data.audit.RemoteAuditRepository;
import br.com.tresvtintas.mobile.data.audit.RemoteAgentReplayRepository;
import br.com.tresvtintas.mobile.data.catalog.CatalogAccountScope;
import br.com.tresvtintas.mobile.data.catalog.CatalogDataEnvironment;
import br.com.tresvtintas.mobile.data.catalogadmin.CatalogAdministrationAccountScope;
import br.com.tresvtintas.mobile.data.catalogadmin.RemoteCatalogAdministrationRepository;
import br.com.tresvtintas.mobile.data.catalogadmin.RemoteCatalogImportRepository;
import br.com.tresvtintas.mobile.data.commission.CommissionAccountScope;
import br.com.tresvtintas.mobile.data.commission.RemoteCommissionRepository;
import br.com.tresvtintas.mobile.data.customer.CustomerAccountScope;
import br.com.tresvtintas.mobile.data.customer.CustomerDataEnvironment;
import br.com.tresvtintas.mobile.data.customer.RemoteCustomerRepository;
import br.com.tresvtintas.mobile.data.dashboard.DashboardAccountScope;
import br.com.tresvtintas.mobile.data.dashboard.RemoteDashboardRepository;
import br.com.tresvtintas.mobile.data.team.RemoteTeamRepository;
import br.com.tresvtintas.mobile.data.team.TeamAccountScope;
import br.com.tresvtintas.mobile.data.painteradmin.PainterAdministrationAccountScope;
import br.com.tresvtintas.mobile.data.painteradmin.RemotePainterAdministrationRepository;
import br.com.tresvtintas.mobile.data.useradmin.RemoteUserAdministrationRepository;
import br.com.tresvtintas.mobile.data.useradmin.UserAdministrationAccountScope;
import br.com.tresvtintas.mobile.data.organizationadmin.OrganizationAdministrationAccountScope;
import br.com.tresvtintas.mobile.data.organizationadmin.RemoteOrganizationAdministrationRepository;
import br.com.tresvtintas.mobile.data.systemconfiguration.RemoteSystemConfigurationRepository;
import br.com.tresvtintas.mobile.data.systemconfiguration.SystemConfigurationAccountScope;
import br.com.tresvtintas.mobile.data.whatsappadmin.RemoteWhatsAppAdministrationRepository;
import br.com.tresvtintas.mobile.data.whatsappadmin.WhatsAppAdministrationAccountScope;
import br.com.tresvtintas.mobile.data.delivery.DeliveryAccountScope;
import br.com.tresvtintas.mobile.data.delivery.RemoteDeliveryRepository;
import br.com.tresvtintas.mobile.data.delivery.RemoteDeliveryManagementRepository;
import br.com.tresvtintas.mobile.data.finance.FinanceAccountScope;
import br.com.tresvtintas.mobile.data.finance.CorporateFinanceAccountScope;
import br.com.tresvtintas.mobile.data.finance.RemoteCorporateFinanceOrganizationRepository;
import br.com.tresvtintas.mobile.data.finance.RemoteCorporateFinanceRepository;
import br.com.tresvtintas.mobile.data.finance.RemoteFinanceRepository;
import br.com.tresvtintas.mobile.data.laborquote.LaborQuoteAccountScope;
import br.com.tresvtintas.mobile.data.laborquote.RemoteLaborQuoteRepository;
import br.com.tresvtintas.mobile.data.quote.MaterialQuoteAccountScope;
import br.com.tresvtintas.mobile.data.quote.RemoteMaterialQuoteRepository;
import br.com.tresvtintas.mobile.data.order.OrderAccountScope;
import br.com.tresvtintas.mobile.data.order.RemoteOrderRepository;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessFeatureRuntime;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessActivity;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessRuntimeProvider;
import br.com.tresvtintas.mobile.feature.accountaccess.ManagedAccountAccessFeatureRuntime;
import br.com.tresvtintas.mobile.feature.accountaccess.ManagedAccountAccessRuntimeProvider;
import br.com.tresvtintas.mobile.feature.agent.AgentFeatureRuntime;
import br.com.tresvtintas.mobile.feature.agent.AgentRuntimeProvider;
import br.com.tresvtintas.mobile.feature.catalog.CatalogRuntimeProvider;
import br.com.tresvtintas.mobile.feature.catalogadmin.CatalogAdministrationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.catalogadmin.CatalogAdministrationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceFeatureRuntime;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceRuntimeProvider;
import br.com.tresvtintas.mobile.feature.appointment.AppointmentFeatureRuntime;
import br.com.tresvtintas.mobile.feature.appointment.AppointmentRuntimeProvider;
import br.com.tresvtintas.mobile.feature.audit.AuditFeatureRuntime;
import br.com.tresvtintas.mobile.feature.audit.AuditRuntimeProvider;
import br.com.tresvtintas.mobile.feature.commission.CommissionFeatureRuntime;
import br.com.tresvtintas.mobile.feature.commission.CommissionRuntimeProvider;
import br.com.tresvtintas.mobile.feature.corporatefinance.CorporateFinanceFeatureRuntime;
import br.com.tresvtintas.mobile.feature.corporatefinance.CorporateFinanceRuntimeProvider;
import br.com.tresvtintas.mobile.feature.customer.CustomerFeatureRuntime;
import br.com.tresvtintas.mobile.feature.customer.CustomerRuntimeProvider;
import br.com.tresvtintas.mobile.feature.dashboard.DashboardFeatureRuntime;
import br.com.tresvtintas.mobile.feature.dashboard.DashboardRuntimeProvider;
import br.com.tresvtintas.mobile.feature.team.TeamFeatureRuntime;
import br.com.tresvtintas.mobile.feature.team.TeamRuntimeProvider;
import br.com.tresvtintas.mobile.feature.painteradmin.PainterAdministrationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.painteradmin.PainterAdministrationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.useradmin.UserAdministrationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.useradmin.UserAdministrationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.organizationadmin.OrganizationAdministrationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.organizationadmin.OrganizationAdministrationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryFeatureRuntime;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryRuntimeProvider;
import br.com.tresvtintas.mobile.feature.finance.FinanceFeatureRuntime;
import br.com.tresvtintas.mobile.feature.finance.FinanceRuntimeProvider;
import br.com.tresvtintas.mobile.feature.finance.FinanceRoute;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteFeatureRuntime;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuotePdfCache;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteRuntimeProvider;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuoteFeatureRuntime;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuotePdfCache;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuoteRuntimeProvider;
import br.com.tresvtintas.mobile.feature.order.OrderFeatureRuntime;
import br.com.tresvtintas.mobile.feature.order.OrderRuntimeProvider;
import br.com.tresvtintas.mobile.feature.notifications.NotificationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.notifications.NotificationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellMenuPolicy;
import br.com.tresvtintas.mobile.feature.systemconfiguration.SystemConfigurationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.systemconfiguration.SystemConfigurationRuntimeProvider;
import br.com.tresvtintas.mobile.feature.whatsappadmin.WhatsAppAdministrationFeatureRuntime;
import br.com.tresvtintas.mobile.feature.whatsappadmin.WhatsAppAdministrationRuntimeProvider;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreeVTintasApplication extends Application
        implements CatalogRuntimeProvider, CatalogAdministrationRuntimeProvider,
        CustomerRuntimeProvider,
        MaterialQuoteRuntimeProvider, LaborQuoteRuntimeProvider, OrderRuntimeProvider,
        CommissionRuntimeProvider, FinanceRuntimeProvider,
        CorporateFinanceRuntimeProvider,
        AppointmentRuntimeProvider, DeliveryRuntimeProvider,
        AttendanceRuntimeProvider, AgentRuntimeProvider,
        AccountAccessRuntimeProvider, ManagedAccountAccessRuntimeProvider,
        DashboardRuntimeProvider,
        TeamRuntimeProvider,
        PainterAdministrationRuntimeProvider,
        UserAdministrationRuntimeProvider,
        OrganizationAdministrationRuntimeProvider,
        AuditRuntimeProvider,
        NotificationRuntimeProvider,
        SystemConfigurationRuntimeProvider,
        WhatsAppAdministrationRuntimeProvider,
        WorkforceLocationHost {
    private static final int APPOINTMENT_WORKER_COUNT = 3;
    private static final Set<MobileArea> BASE_IMPLEMENTED_AREAS = Set.of(
            MobileArea.ACCOUNT_SECURITY,
            MobileArea.DASHBOARD,
            MobileArea.TEAM,
            MobileArea.PAINTERS_AND_APPROVALS,
            MobileArea.TEAM_AND_USERS,
            MobileArea.ORGANIZATION_ADMINISTRATION,
            MobileArea.AUDIT,
            MobileArea.CATALOG,
            MobileArea.CUSTOMERS,
            MobileArea.MATERIAL_QUOTES,
            MobileArea.LABOR_QUOTES,
            MobileArea.ORDERS,
            MobileArea.DELIVERIES,
            MobileArea.COMMISSIONS,
            MobileArea.COMMISSION_TEAM,
            MobileArea.PERSONAL_FINANCE,
            MobileArea.CORPORATE_FINANCE,
            MobileArea.AGENDA,
            MobileArea.TEAM_AGENDA,
            MobileArea.GLOBAL_AGENDA,
            MobileArea.CUSTOMER_SERVICE,
            MobileArea.PERSONAL_AI_AGENT,
            MobileArea.NOTIFICATIONS,
            MobileArea.WHATSAPP_INTEGRATIONS,
            MobileArea.SETTINGS_AND_PROFILE);
    private static final Set<MobileArea> IMPLEMENTED_AREAS = implementedAreas();
    private final AtomicBoolean restoreStarted = new AtomicBoolean();
    private final AtomicBoolean authorizedPromptAttempted = new AtomicBoolean();
    private final AtomicBoolean workforceRecoveryStarted = new AtomicBoolean();
    private final AtomicInteger appointmentWorkerSequence =
            new AtomicInteger();
    private final AuthStateListener workforceAuthListener =
            this::reconcileWorkforceAuthentication;
    private final BootstrapStateListener workforceBootstrapListener =
            this::reconcileWorkforceBootstrap;
    private AuthController authController;
    private BootstrapController bootstrapController;
    private AuthFailureKind authStartupFailure;
    private ExecutorService authWorker;
    private ExecutorService catalogWorker;
    private ExecutorService catalogAdministrationWorker;
    private ExecutorService customerWorker;
    private ExecutorService quoteWorker;
    private ExecutorService laborQuoteWorker;
    private ExecutorService orderWorker;
    private ExecutorService deliveryWorker;
    private ExecutorService dashboardWorker;
    private ExecutorService teamWorker;
    private ExecutorService painterAdministrationWorker;
    private ExecutorService userAdministrationWorker;
    private ExecutorService organizationAdministrationWorker;
    private ExecutorService systemConfigurationWorker;
    private ExecutorService whatsAppAdministrationWorker;
    private ExecutorService auditWorker;
    private ExecutorService commissionWorker;
    private ExecutorService financeWorker;
    private ExecutorService appointmentWorker;
    private MobileApi protectedApi;
    private GoogleIdTokenRequester googleIdTokenRequester;
    private AccountAccessApplicationComponent accountAccessComponent;
    private AttendanceApplicationComponent attendanceComponent;
    private AgentApplicationComponent agentComponent;
    private NotificationsApplicationComponent notificationsComponent;
    private CatalogDataEnvironment catalogDataEnvironment;
    private Optional<CatalogSession> catalogSession = Optional.empty();
    private Optional<CatalogAdministrationSession>
            catalogAdministrationSession = Optional.empty();
    private Optional<CustomerSession> customerSession = Optional.empty();
    private Optional<MaterialQuoteSession> materialQuoteSession = Optional.empty();
    private Optional<LaborQuoteSession> laborQuoteSession = Optional.empty();
    private Optional<OrderSession> orderSession = Optional.empty();
    private Optional<DeliverySession> deliverySession =
            Optional.empty();
    private Optional<DashboardSession> dashboardSession = Optional.empty();
    private Optional<TeamSession> teamSession = Optional.empty();
    private Optional<PainterAdministrationSession>
            painterAdministrationSession = Optional.empty();
    private Optional<UserAdministrationSession>
            userAdministrationSession = Optional.empty();
    private Optional<OrganizationAdministrationSession>
            organizationAdministrationSession = Optional.empty();
    private Optional<SystemConfigurationSession>
            systemConfigurationSession = Optional.empty();
    private Optional<WhatsAppAdministrationSession>
            whatsAppAdministrationSession = Optional.empty();
    private Optional<AuditSession> auditSession = Optional.empty();
    private Optional<CommissionSession> commissionSession = Optional.empty();
    private Optional<FinanceSession> financeSession = Optional.empty();
    private Optional<CorporateFinanceDirectorySession>
            corporateFinanceDirectorySession = Optional.empty();
    private Optional<CorporateFinanceEntrySession>
            corporateFinanceEntrySession = Optional.empty();
    private Optional<AppointmentSession> appointmentSession =
            Optional.empty();
    private OptionalLong workforceLocationOrganizationId = OptionalLong.empty();

    @Override
    public void onCreate() {
        super.onCreate();
        MaterialQuotePdfCache.clear(this);
        LaborQuotePdfCache.clear(this);
        if (!BuildConfig.MOBILE_API_CONFIGURED) {
            authStartupFailure = AuthFailureKind.CONFIGURATION;
            return;
        }
        authWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-auth-worker");
            thread.setDaemon(false);
            return thread;
        });
        catalogWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-catalog-worker");
            thread.setDaemon(false);
            return thread;
        });
        catalogAdministrationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-catalog-administration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        customerWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-customer-worker");
            thread.setDaemon(false);
            return thread;
        });
        quoteWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-material-quote-worker");
            thread.setDaemon(false);
            return thread;
        });
        laborQuoteWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-labor-quote-worker");
            thread.setDaemon(false);
            return thread;
        });
        orderWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-order-worker");
            thread.setDaemon(false);
            return thread;
        });
        deliveryWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "3v-delivery-worker");
            thread.setDaemon(false);
            return thread;
        });
        dashboardWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-dashboard-worker");
            thread.setDaemon(false);
            return thread;
        });
        teamWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-team-worker");
            thread.setDaemon(false);
            return thread;
        });
        painterAdministrationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-painter-administration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        userAdministrationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-user-administration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        organizationAdministrationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-organization-administration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        systemConfigurationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-system-configuration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        whatsAppAdministrationWorker =
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-whatsapp-administration-worker");
                    thread.setDaemon(false);
                    return thread;
                });
        auditWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-audit-worker");
            thread.setDaemon(false);
            return thread;
        });
        commissionWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-commission-worker");
            thread.setDaemon(false);
            return thread;
        });
        financeWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "3v-finance-worker");
            thread.setDaemon(false);
            return thread;
        });
        appointmentWorker = Executors.newFixedThreadPool(
                APPOINTMENT_WORKER_COUNT,
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "3v-appointment-worker-"
                                    + appointmentWorkerSequence.incrementAndGet());
                    thread.setDaemon(false);
                    return thread;
                });
        try {
            NetworkConfiguration configuration = new NetworkConfiguration(
                    BuildConfig.MOBILE_API_BASE_URL,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE);
            AuthRuntime authRuntime = AuthRuntimeFactory.create(
                    this,
                    configuration,
                    BuildConfig.VERSION_NAME,
                    authWorker,
                    ContextCompat.getMainExecutor(this));
            authController = authRuntime.controller();
            protectedApi = authRuntime.protectedApi();
            googleIdTokenRequester =
                    new CredentialManagerGoogleIdTokenRequester(
                            this,
                            ContextCompat.getMainExecutor(this));
            accountAccessComponent =
                    new AccountAccessApplicationComponent(
                            protectedApi,
                            this::rejectAccountSession);
            attendanceComponent =
                    new AttendanceApplicationComponent(
                            authRuntime.protectedClient(),
                            ContextCompat.getMainExecutor(this));
            agentComponent = new AgentApplicationComponent(
                    authRuntime.protectedClient(),
                    ContextCompat.getMainExecutor(this),
                    googleIdTokenRequester);
            notificationsComponent = new NotificationsApplicationComponent(
                    this,
                    protectedApi,
                    ContextCompat.getMainExecutor(this),
                    new FirebaseClientConfiguration(
                            BuildConfig.FIREBASE_CONFIGURED,
                            BuildConfig.FIREBASE_APPLICATION_ID,
                            BuildConfig.FIREBASE_PROJECT_ID,
                            BuildConfig.FIREBASE_API_KEY,
                            BuildConfig.FIREBASE_SENDER_ID));
            bootstrapController = BootstrapRuntimeFactory.create(
                    protectedApi,
                    new ClientCompatibility(
                            "v1",
                            BuildConfig.MOBILE_CONTRACT_VERSION,
                            BuildConfig.VERSION_CODE,
                            Build.VERSION.SDK_INT),
                    authWorker,
                    ContextCompat.getMainExecutor(this));
            startWorkforceRuntimeRecovery();
        } catch (AuthException exception) {
            authStartupFailure = exception.kind();
            if (accountAccessComponent != null) {
                accountAccessComponent.close();
            }
            if (attendanceComponent != null) {
                attendanceComponent.close();
            }
            if (agentComponent != null) {
                agentComponent.close();
            }
            if (notificationsComponent != null) {
                notificationsComponent.close();
            }
            authWorker.shutdownNow();
            catalogWorker.shutdownNow();
            catalogAdministrationWorker.shutdownNow();
            customerWorker.shutdownNow();
            quoteWorker.shutdownNow();
            laborQuoteWorker.shutdownNow();
            orderWorker.shutdownNow();
            deliveryWorker.shutdownNow();
            dashboardWorker.shutdownNow();
            teamWorker.shutdownNow();
            painterAdministrationWorker.shutdownNow();
            userAdministrationWorker.shutdownNow();
            organizationAdministrationWorker.shutdownNow();
            systemConfigurationWorker.shutdownNow();
            whatsAppAdministrationWorker.shutdownNow();
            auditWorker.shutdownNow();
            commissionWorker.shutdownNow();
            financeWorker.shutdownNow();
            appointmentWorker.shutdownNow();
        } catch (IllegalArgumentException exception) {
            authStartupFailure = AuthFailureKind.CONFIGURATION;
            if (accountAccessComponent != null) {
                accountAccessComponent.close();
            }
            if (attendanceComponent != null) {
                attendanceComponent.close();
            }
            if (agentComponent != null) {
                agentComponent.close();
            }
            if (notificationsComponent != null) {
                notificationsComponent.close();
            }
            authWorker.shutdownNow();
            catalogWorker.shutdownNow();
            catalogAdministrationWorker.shutdownNow();
            customerWorker.shutdownNow();
            quoteWorker.shutdownNow();
            laborQuoteWorker.shutdownNow();
            orderWorker.shutdownNow();
            deliveryWorker.shutdownNow();
            dashboardWorker.shutdownNow();
            teamWorker.shutdownNow();
            painterAdministrationWorker.shutdownNow();
            userAdministrationWorker.shutdownNow();
            organizationAdministrationWorker.shutdownNow();
            systemConfigurationWorker.shutdownNow();
            whatsAppAdministrationWorker.shutdownNow();
            auditWorker.shutdownNow();
            commissionWorker.shutdownNow();
            financeWorker.shutdownNow();
            appointmentWorker.shutdownNow();
        }
    }

    public Optional<AuthController> authController() {
        return Optional.ofNullable(authController);
    }

    public Optional<AuthFailureKind> authStartupFailure() {
        return Optional.ofNullable(authStartupFailure);
    }

    public Optional<BootstrapController> bootstrapController() {
        return Optional.ofNullable(bootstrapController);
    }

    public void restoreAuthenticationOnce() {
        if (authController != null && restoreStarted.compareAndSet(false, true)) {
            authController.restore();
        }
    }

    private void startWorkforceRuntimeRecovery() {
        if (!BuildConfig.IS_WORKFORCE
                || authController == null
                || bootstrapController == null
                || !workforceRecoveryStarted.compareAndSet(false, true)) {
            return;
        }
        bootstrapController.subscribe(workforceBootstrapListener);
        authController.subscribe(workforceAuthListener);
        restoreAuthenticationOnce();
    }

    private void reconcileWorkforceAuthentication(AuthState state) {
        if (!BuildConfig.IS_WORKFORCE || bootstrapController == null) {
            return;
        }
        if (state.phase() == AuthState.Phase.AUTHENTICATED) {
            bootstrapController.load(expectedBootstrapIdentity(
                    state.session().orElseThrow()));
            return;
        }
        deactivateWorkforceLocation();
        if (state.phase() != AuthState.Phase.RESTORING
                && bootstrapController.currentState().phase()
                        != BootstrapState.Phase.EMPTY) {
            bootstrapController.clear();
        }
    }

    private void reconcileWorkforceBootstrap(BootstrapState state) {
        if (!BuildConfig.IS_WORKFORCE) {
            return;
        }
        if (state.phase() != BootstrapState.Phase.READY) {
            deactivateWorkforceLocation();
            return;
        }
        updateRecoveredWorkforceOrganization(
                WorkforceRuntimeScopeResolver.resolve(
                        state.snapshot().orElseThrow()));
    }

    private synchronized void updateRecoveredWorkforceOrganization(
            OptionalLong organizationId) {
        workforceLocationOrganizationId = organizationId;
    }

    public boolean shouldAttemptAuthorizedPrompt() {
        return authController != null
                && authorizedPromptAttempted.compareAndSet(false, true);
    }

    public synchronized void activateCatalog(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(access.bootstrap().authorization(), IMPLEMENTED_AREAS)
                        .contains(MobileArea.CATALOG)
                || access.selectedOrganization().isEmpty()
                || protectedApi == null
                || catalogWorker == null) {
            deactivateCatalog(false);
            return;
        }
        CatalogAccountScope scope = CatalogAccountScope.from(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                access.selectedOrganization().orElseThrow().id());
        if (catalogSession.map(CatalogSession::scope)
                .filter(current -> sameScope(current, scope))
                .isPresent()) {
            return;
        }
        deactivateCatalog(false);
        CachedCatalogRepository repository =
                catalogDataEnvironment().repository(
                        scope,
                        protectedApi,
                        access.bootstrap().authorization().has(Capability.PRICING_READ));
        CatalogController controller = new CatalogController(
                repository,
                catalogWorker,
                ContextCompat.getMainExecutor(this));
        catalogSession = Optional.of(new CatalogSession(scope, repository, controller));
    }

    public synchronized void deactivateCatalog(boolean clearAccountData) {
        Optional<CatalogSession> previous = catalogSession;
        catalogSession = Optional.empty();
        if (previous.isEmpty()) {
            return;
        }
        CatalogSession session = previous.orElseThrow();
        session.controller().close();
        if (clearAccountData && catalogWorker != null) {
            catalogWorker.execute(() -> clearCatalogAccount(session.repository()));
        }
    }

    public synchronized void activateCatalogAdministration(
            ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !access.bootstrap()
                        .authorization()
                        .has(Capability.CATALOG_MANAGE)
                || protectedApi == null
                || catalogAdministrationWorker == null) {
            deactivateCatalogAdministration();
            return;
        }
        CatalogAdministrationAccountScope scope =
                new CatalogAdministrationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (catalogAdministrationSession
                .map(CatalogAdministrationSession::scope)
                .filter(existingScope -> existingScope.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateCatalogAdministration();
        RemoteCatalogAdministrationRepository repository =
                new RemoteCatalogAdministrationRepository(
                        scope,
                        protectedApi);
        RemoteCatalogImportRepository importRepository =
                new RemoteCatalogImportRepository(
                        scope,
                        protectedApi);
        CatalogAdministrationFeatureRuntime runtime =
                new CatalogAdministrationFeatureRuntime(
                        repository,
                        importRepository,
                        catalogAdministrationWorker,
                        scope.actorUserId());
        catalogAdministrationSession = Optional.of(
                new CatalogAdministrationSession(
                        scope,
                        repository,
                        importRepository,
                        runtime));
    }

    public synchronized void deactivateCatalogAdministration() {
        Optional<CatalogAdministrationSession> previous =
                catalogAdministrationSession;
        catalogAdministrationSession = Optional.empty();
        previous.ifPresent(session -> {
            session.repository().close();
            session.importRepository().close();
        });
    }

    public synchronized void activateCustomers(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(
                                access.bootstrap().authorization(),
                                IMPLEMENTED_AREAS)
                        .contains(MobileArea.CUSTOMERS)
                || protectedApi == null
                || customerWorker == null) {
            deactivateCustomers();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        CustomerAccountScope scope = new CustomerAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                organizationId);
        if (customerSession.map(CustomerSession::scope)
                .filter(current -> current.accountKey().equals(scope.accountKey()))
                .isPresent()) {
            return;
        }
        deactivateCustomers();
        RemoteCustomerRepository repository =
                CustomerDataEnvironment.repository(scope, protectedApi);
        CustomerFeatureRuntime featureRuntime = new CustomerFeatureRuntime(
                repository,
                customerWorker,
                organizationId,
                access.bootstrap().authorization().has(
                        Capability.CUSTOMER_WRITE));
        customerSession = Optional.of(new CustomerSession(
                scope,
                repository,
                featureRuntime));
    }

    public synchronized void deactivateCustomers() {
        deactivateMaterialQuotes();
        deactivateLaborQuotes();
        Optional<CustomerSession> previous = customerSession;
        customerSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateMaterialQuotes(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(
                                access.bootstrap().authorization(),
                                IMPLEMENTED_AREAS)
                        .contains(MobileArea.MATERIAL_QUOTES)
                || protectedApi == null
                || quoteWorker == null
                || customerSession.isEmpty()
                || catalogSession.isEmpty()
                || orderSession.isEmpty()) {
            deactivateMaterialQuotes();
            return;
        }
        MaterialQuoteAccountScope scope = new MaterialQuoteAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (materialQuoteSession.map(MaterialQuoteSession::scope)
                .filter(current -> current.accountKey().equals(scope.accountKey()))
                .isPresent()) {
            return;
        }
        deactivateMaterialQuotes();
        RemoteMaterialQuoteRepository repository =
                new RemoteMaterialQuoteRepository(scope, protectedApi);
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        MaterialQuoteFeatureRuntime featureRuntime =
                new MaterialQuoteFeatureRuntime(
                        repository,
                        customerSession.orElseThrow().repository(),
                        catalogSession.orElseThrow().repository(),
                        orderSession.orElseThrow().repository(),
                        quoteWorker,
                        organizationId,
                        access.bootstrap().authorization().has(
                                Capability.QUOTE_CREATE),
                        access.bootstrap().authorization().has(
                                Capability.QUOTE_STATUS_WRITE),
                        access.bootstrap().authorization().has(
                                Capability.QUOTE_PDF_READ),
                        access.bootstrap().authorization().has(
                                Capability.ORDER_CREATE));
        materialQuoteSession = Optional.of(new MaterialQuoteSession(
                scope,
                repository,
                featureRuntime));
    }

    public synchronized void deactivateMaterialQuotes() {
        Optional<MaterialQuoteSession> previous = materialQuoteSession;
        materialQuoteSession = Optional.empty();
        previous.ifPresent(session -> {
            session.repository().close();
            if (quoteWorker != null && !quoteWorker.isShutdown()) {
                quoteWorker.execute(() -> MaterialQuotePdfCache.clear(this));
            }
        });
    }

    public synchronized void activateLaborQuotes(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(
                                access.bootstrap().authorization(),
                                IMPLEMENTED_AREAS)
                        .contains(MobileArea.LABOR_QUOTES)
                || protectedApi == null
                || laborQuoteWorker == null
                || customerSession.isEmpty()) {
            deactivateLaborQuotes();
            return;
        }
        LaborQuoteAccountScope scope = new LaborQuoteAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (laborQuoteSession.map(LaborQuoteSession::scope)
                .filter(current -> current.accountKey().equals(scope.accountKey()))
                .isPresent()) {
            return;
        }
        deactivateLaborQuotes();
        RemoteLaborQuoteRepository repository =
                new RemoteLaborQuoteRepository(scope, protectedApi);
        LaborQuoteFeatureRuntime featureRuntime = new LaborQuoteFeatureRuntime(
                repository,
                customerSession.orElseThrow().repository(),
                laborQuoteWorker,
                access.bootstrap().authorization().has(
                        Capability.LABOR_QUOTE_CREATE),
                access.bootstrap().authorization().has(
                        Capability.LABOR_QUOTE_STATUS_WRITE),
                access.bootstrap().authorization().has(
                        Capability.LABOR_QUOTE_PDF_READ));
        laborQuoteSession = Optional.of(new LaborQuoteSession(
                scope,
                repository,
                featureRuntime));
    }

    public synchronized void deactivateLaborQuotes() {
        Optional<LaborQuoteSession> previous = laborQuoteSession;
        laborQuoteSession = Optional.empty();
        previous.ifPresent(session -> {
            session.repository().close();
            if (laborQuoteWorker != null && !laborQuoteWorker.isShutdown()) {
                laborQuoteWorker.execute(() -> LaborQuotePdfCache.clear(this));
            }
        });
    }

    public synchronized void activateOrders(ShellAccessState access) {
        if (access == null || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(access.bootstrap().authorization(), IMPLEMENTED_AREAS)
                        .contains(MobileArea.ORDERS)
                || protectedApi == null || orderWorker == null) {
            deactivateOrders();
            return;
        }
        OrderAccountScope scope = new OrderAccountScope(access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (orderSession.map(OrderSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateOrders();
        RemoteOrderRepository repository = new RemoteOrderRepository(
                scope,
                protectedApi,
                access.bootstrap().authorization().has(Capability.PRICING_READ));
        orderSession = Optional.of(new OrderSession(scope, repository,
                new OrderFeatureRuntime(repository, orderWorker)));
    }

    public synchronized void deactivateOrders() {
        deactivateMaterialQuotes();
        Optional<OrderSession> previous = orderSession;
        orderSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateDeliveries(
            ShellAccessState access) {
        if (access == null || !access.isOperational()
                || !ShellMenuPolicy.enabledAreas(
                                access.bootstrap().authorization(),
                                IMPLEMENTED_AREAS)
                        .contains(MobileArea.DELIVERIES)
                || protectedApi == null
                || deliveryWorker == null) {
            deactivateDeliveries();
            return;
        }
        DeliveryAccountScope scope = new DeliveryAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (deliverySession.map(DeliverySession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateDeliveries();
        RemoteDeliveryRepository repository =
                new RemoteDeliveryRepository(scope, protectedApi);
        boolean canSchedule = access.bootstrap().authorization().has(
                Capability.DELIVERY_SCHEDULE);
        boolean canAssign = access.bootstrap().authorization().has(
                Capability.DELIVERY_ASSIGN);
        boolean canCompleteManagement =
                access.bootstrap().authorization().has(
                        Capability.DELIVERY_COMPLETE_MANAGEMENT);
        Optional<RemoteDeliveryManagementRepository> managementRepository =
                canSchedule || canAssign || canCompleteManagement
                        ? Optional.of(new RemoteDeliveryManagementRepository(
                                scope,
                                protectedApi))
                        : Optional.empty();
        deliverySession = Optional.of(new DeliverySession(
                scope,
                repository,
                managementRepository,
                new DeliveryFeatureRuntime(
                        repository,
                        managementRepository.map(value -> value),
                        access.bootstrap().role() == AppRole.DELIVERY_DRIVER
                                ? Optional.of(repository)
                                : Optional.empty(),
                        canSchedule,
                        canAssign,
                        canCompleteManagement,
                        deliveryWorker)));
    }

    public synchronized void deactivateDeliveries() {
        Optional<DeliverySession> previous = deliverySession;
        deliverySession = Optional.empty();
        previous.ifPresent(session -> {
            session.repository().close();
            session.managementRepository().ifPresent(
                    RemoteDeliveryManagementRepository::close);
        });
    }

    public synchronized void activateDashboard(ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.DASHBOARD)
                || protectedApi == null
                || dashboardWorker == null) {
            deactivateDashboard();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        DashboardAccountScope scope = new DashboardAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                organizationId);
        if (dashboardSession.map(DashboardSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateDashboard();
        RemoteDashboardRepository repository =
                new RemoteDashboardRepository(scope, protectedApi);
        dashboardSession = Optional.of(new DashboardSession(
                scope,
                repository,
                new DashboardFeatureRuntime(
                        repository,
                        dashboardWorker)));
    }

    public synchronized void deactivateDashboard() {
        Optional<DashboardSession> previous = dashboardSession;
        dashboardSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateTeam(ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.TEAM)
                || protectedApi == null
                || teamWorker == null) {
            deactivateTeam();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        TeamAccountScope scope = new TeamAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                organizationId);
        if (teamSession.map(TeamSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateTeam();
        RemoteTeamRepository repository =
                new RemoteTeamRepository(scope, protectedApi);
        teamSession = Optional.of(new TeamSession(
                scope,
                repository,
                new TeamFeatureRuntime(repository, teamWorker)));
    }

    public synchronized void deactivateTeam() {
        Optional<TeamSession> previous = teamSession;
        teamSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activatePainterAdministration(
            ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.PAINTERS_AND_APPROVALS)
                || protectedApi == null
                || painterAdministrationWorker == null) {
            deactivatePainterAdministration();
            return;
        }
        PainterAdministrationAccountScope scope =
                new PainterAdministrationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (painterAdministrationSession
                .map(PainterAdministrationSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivatePainterAdministration();
        RemotePainterAdministrationRepository repository =
                new RemotePainterAdministrationRepository(
                        scope,
                        protectedApi);
        painterAdministrationSession = Optional.of(
                new PainterAdministrationSession(
                        scope,
                        repository,
                        new PainterAdministrationFeatureRuntime(
                                repository,
                                painterAdministrationWorker)));
    }

    public synchronized void deactivatePainterAdministration() {
        Optional<PainterAdministrationSession> previous =
                painterAdministrationSession;
        painterAdministrationSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateUserAdministration(
            ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.TEAM_AND_USERS)
                || protectedApi == null
                || userAdministrationWorker == null) {
            deactivateUserAdministration();
            return;
        }
        UserAdministrationAccountScope scope =
                new UserAdministrationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (userAdministrationSession
                .map(UserAdministrationSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivateUserAdministration();
        RemoteUserAdministrationRepository repository =
                new RemoteUserAdministrationRepository(scope, protectedApi);
        boolean canManageSecurity = access.bootstrap()
                .authorization()
                .has(Capability.SECURITY_MANAGE_USERS);
        userAdministrationSession = Optional.of(new UserAdministrationSession(
                scope,
                repository,
                new UserAdministrationFeatureRuntime(
                        repository,
                        userAdministrationWorker,
                        scope.userId(),
                        canManageSecurity
                                ? Optional.of((context, userId, userName) ->
                                        context.startActivity(
                                                AccountAccessActivity.managedIntent(
                                                        context,
                                                        userId,
                                                        userName)))
                                : Optional.empty())));
    }

    public synchronized void deactivateUserAdministration() {
        Optional<UserAdministrationSession> previous = userAdministrationSession;
        userAdministrationSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateOrganizationAdministration(
            ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.ORGANIZATION_ADMINISTRATION)
                || protectedApi == null
                || organizationAdministrationWorker == null) {
            deactivateOrganizationAdministration();
            return;
        }
        OrganizationAdministrationAccountScope scope =
                new OrganizationAdministrationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (organizationAdministrationSession
                .map(OrganizationAdministrationSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivateOrganizationAdministration();
        RemoteOrganizationAdministrationRepository repository =
                new RemoteOrganizationAdministrationRepository(
                        scope,
                        protectedApi);
        organizationAdministrationSession = Optional.of(
                new OrganizationAdministrationSession(
                        scope,
                        repository,
                        new OrganizationAdministrationFeatureRuntime(
                                repository,
                                organizationAdministrationWorker,
                                scope.actorUserId())));
    }

    public synchronized void deactivateOrganizationAdministration() {
        Optional<OrganizationAdministrationSession> previous =
                organizationAdministrationSession;
        organizationAdministrationSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateSystemConfiguration(
            ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.SETTINGS_AND_PROFILE)
                || protectedApi == null
                || systemConfigurationWorker == null) {
            deactivateSystemConfiguration();
            return;
        }
        SystemConfigurationAccountScope scope =
                new SystemConfigurationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (systemConfigurationSession
                .map(SystemConfigurationSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivateSystemConfiguration();
        RemoteSystemConfigurationRepository repository =
                new RemoteSystemConfigurationRepository(scope, protectedApi);
        SystemConfigurationController controller =
                new SystemConfigurationController(
                        repository,
                        systemConfigurationWorker,
                        ContextCompat.getMainExecutor(this));
        systemConfigurationSession = Optional.of(
                new SystemConfigurationSession(
                        scope,
                        repository,
                        controller,
                        new SystemConfigurationFeatureRuntime(controller)));
    }

    public synchronized void deactivateSystemConfiguration() {
        Optional<SystemConfigurationSession> previous =
                systemConfigurationSession;
        systemConfigurationSession = Optional.empty();
        previous.ifPresent(session -> {
            session.controller().close();
            session.repository().close();
        });
    }

    public synchronized void activateWhatsAppAdministration(
            ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.WHATSAPP_INTEGRATIONS)
                || protectedApi == null
                || whatsAppAdministrationWorker == null) {
            deactivateWhatsAppAdministration();
            return;
        }
        WhatsAppAdministrationAccountScope scope =
                new WhatsAppAdministrationAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision());
        if (whatsAppAdministrationSession
                .map(WhatsAppAdministrationSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivateWhatsAppAdministration();
        RemoteWhatsAppAdministrationRepository repository =
                new RemoteWhatsAppAdministrationRepository(scope, protectedApi);
        whatsAppAdministrationSession = Optional.of(
                new WhatsAppAdministrationSession(
                        scope,
                        repository,
                        new WhatsAppAdministrationFeatureRuntime(
                                repository,
                                whatsAppAdministrationWorker)));
    }

    public synchronized void deactivateWhatsAppAdministration() {
        Optional<WhatsAppAdministrationSession> previous =
                whatsAppAdministrationSession;
        whatsAppAdministrationSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateAudit(ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || !enabled.contains(MobileArea.AUDIT)
                || protectedApi == null
                || auditWorker == null) {
            deactivateAudit();
            return;
        }
        AuditAccountScope scope = new AuditAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (auditSession.map(AuditSession::scope)
                .filter(existing -> scope.equals(existing))
                .isPresent()) {
            return;
        }
        deactivateAudit();
        RemoteAuditRepository repository = new RemoteAuditRepository(
                scope,
                protectedApi);
        RemoteAgentReplayRepository replayRepository =
                access.bootstrap().authorization().has(Capability.AUDIT_REPLAY_READ)
                        ? new RemoteAgentReplayRepository(scope, protectedApi)
                        : null;
        Optional<AgentReplayRepository> replayRuntime = replayRepository == null
                ? Optional.empty()
                : Optional.of(replayRepository);
        auditSession = Optional.of(new AuditSession(
                scope,
                repository,
                Optional.ofNullable(replayRepository),
                new AuditFeatureRuntime(repository, replayRuntime, auditWorker)));
    }

    public synchronized void deactivateAudit() {
        Optional<AuditSession> previous = auditSession;
        auditSession = Optional.empty();
        previous.ifPresent(session -> {
            session.repository().close();
            session.replayRepository().ifPresent(
                    RemoteAgentReplayRepository::close);
        });
    }

    public synchronized void activateCommissions(ShellAccessState access) {
        Set<MobileArea> enabled = access == null
                ? Set.of()
                : ShellMenuPolicy.enabledAreas(
                        access.bootstrap().authorization(),
                        IMPLEMENTED_AREAS);
        if (access == null
                || !access.isOperational()
                || (!enabled.contains(MobileArea.COMMISSIONS)
                        && !enabled.contains(MobileArea.COMMISSION_TEAM))
                || protectedApi == null
                || commissionWorker == null) {
            deactivateCommissions();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        CommissionAccountScope scope = new CommissionAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (commissionSession.map(CommissionSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()
                && commissionSession.orElseThrow().runtime().organizationId()
                        .equals(organizationId)) {
            return;
        }
        deactivateCommissions();
        RemoteCommissionRepository repository =
                new RemoteCommissionRepository(scope, protectedApi);
        commissionSession = Optional.of(new CommissionSession(
                scope,
                repository,
                new CommissionFeatureRuntime(
                        repository,
                        commissionWorker,
                        organizationId)));
    }

    public synchronized void deactivateCommissions() {
        Optional<CommissionSession> previous = commissionSession;
        commissionSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateFinance(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || protectedApi == null
                || financeWorker == null) {
            deactivateFinance();
            return;
        }
        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                access.bootstrap().authorization(),
                IMPLEMENTED_AREAS);
        if (enabled.contains(MobileArea.PERSONAL_FINANCE)) {
            activatePersonalFinance(access);
        } else {
            deactivatePersonalFinance();
        }
        if (enabled.contains(MobileArea.CORPORATE_FINANCE)) {
            activateCorporateFinance(access);
        } else {
            deactivateCorporateFinance();
        }
    }

    private void activatePersonalFinance(ShellAccessState access) {
        FinanceAccountScope scope = new FinanceAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision());
        if (financeSession.map(FinanceSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateFinance();
        RemoteFinanceRepository repository =
                new RemoteFinanceRepository(scope, protectedApi);
        financeSession = Optional.of(new FinanceSession(
                scope,
                repository,
                new FinanceFeatureRuntime(
                        repository,
                        financeWorker,
                        FinanceRoute.personal())));
    }

    private void activateCorporateFinance(ShellAccessState access) {
        boolean globalAccess = access.bootstrap()
                .authorization()
                .organizationAccessMode() == OrganizationAccessMode.ALL;
        CorporateFinanceAccountScope scope =
                new CorporateFinanceAccountScope(
                        access.bootstrap().user().id(),
                        access.bootstrap().authorization().revision(),
                        OptionalLong.empty(),
                        globalAccess);
        if (corporateFinanceDirectorySession
                .map(CorporateFinanceDirectorySession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()) {
            return;
        }
        deactivateCorporateFinance();
        RemoteCorporateFinanceOrganizationRepository repository =
                new RemoteCorporateFinanceOrganizationRepository(
                        scope,
                        protectedApi);
        corporateFinanceDirectorySession = Optional.of(
                new CorporateFinanceDirectorySession(
                        scope,
                        repository,
                        new CorporateFinanceFeatureRuntime(
                                repository,
                                financeWorker,
                                globalAccess)));
    }

    public synchronized void deactivateFinance() {
        deactivatePersonalFinance();
        deactivateCorporateFinance();
    }

    private void deactivatePersonalFinance() {
        Optional<FinanceSession> previous = financeSession;
        financeSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    private void deactivateCorporateFinance() {
        Optional<CorporateFinanceEntrySession> entryPrevious =
                corporateFinanceEntrySession;
        corporateFinanceEntrySession = Optional.empty();
        entryPrevious.ifPresent(session -> session.repository().close());
        Optional<CorporateFinanceDirectorySession> directoryPrevious =
                corporateFinanceDirectorySession;
        corporateFinanceDirectorySession = Optional.empty();
        directoryPrevious.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateAppointments(ShellAccessState access) {
        if (access == null
                || !access.isOperational()
                || protectedApi == null
                || appointmentWorker == null) {
            deactivateAppointments();
            return;
        }
        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                access.bootstrap().authorization(),
                IMPLEMENTED_AREAS);
        AppointmentScope selectedScope;
        Capability writeCapability;
        if (enabled.contains(MobileArea.GLOBAL_AGENDA)) {
            selectedScope = AppointmentScope.ALL;
            writeCapability = Capability.APPOINTMENT_WRITE_ALL;
        } else if (enabled.contains(MobileArea.TEAM_AGENDA)) {
            selectedScope = AppointmentScope.TEAM;
            writeCapability = Capability.APPOINTMENT_WRITE_TEAM;
        } else if (enabled.contains(MobileArea.AGENDA)) {
            selectedScope = AppointmentScope.SELF;
            writeCapability = Capability.APPOINTMENT_WRITE_SELF;
        } else {
            deactivateAppointments();
            return;
        }
        OptionalLong organizationId = access.selectedOrganization()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        AppointmentAccountScope scope = new AppointmentAccountScope(
                access.bootstrap().user().id(),
                access.bootstrap().authorization().revision(),
                selectedScope);
        if (appointmentSession.map(AppointmentSession::scope)
                .filter(current -> current.equals(scope))
                .isPresent()
                && appointmentSession.orElseThrow().runtime().organizationId()
                        .equals(organizationId)) {
            return;
        }
        deactivateAppointments();
        RemoteAppointmentRepository repository =
                new RemoteAppointmentRepository(scope, protectedApi);
        Optional<CustomerRepository> customers =
                customerSession.<CustomerRepository>map(
                        CustomerSession::repository);
        Optional<FinanceFeatureRuntime> agendaFinance =
                agendaFinanceRuntime(access, enabled);
        appointmentSession = Optional.of(new AppointmentSession(
                scope,
                repository,
                new AppointmentFeatureRuntime(
                        repository,
                        appointmentWorker,
                        selectedScope,
                        organizationId,
                        customers,
                        agendaFinance.map(FinanceFeatureRuntime::repository),
                        agendaFinance.map(FinanceFeatureRuntime::route),
                        deliverySession.<DeliveryRepository>map(
                                DeliverySession::repository),
                        access.bootstrap().authorization().has(
                                writeCapability))));
    }

    private Optional<FinanceFeatureRuntime> agendaFinanceRuntime(
            ShellAccessState access,
            Set<MobileArea> enabled) {
        if (enabled.contains(MobileArea.PERSONAL_FINANCE)) {
            return financeRuntime(FinanceRoute.personal());
        }
        if (!enabled.contains(MobileArea.CORPORATE_FINANCE)) {
            return Optional.empty();
        }
        FinanceRoute route = access.selectedOrganization()
                .map(organization -> FinanceRoute.corporate(
                        organization.id(),
                        organization.name()))
                .orElseGet(FinanceRoute::corporateGlobal);
        return financeRuntime(route);
    }

    public synchronized void deactivateAppointments() {
        Optional<AppointmentSession> previous = appointmentSession;
        appointmentSession = Optional.empty();
        previous.ifPresent(session -> session.repository().close());
    }

    public synchronized void activateAttendance(ShellAccessState access) {
        if (attendanceComponent != null) {
            attendanceComponent.activate(access);
        }
    }

    public synchronized void activateAccountAccess(
            ShellAccessState access,
            AuthenticatedSession authenticated) {
        if (accountAccessComponent != null) {
            accountAccessComponent.activate(access, authenticated);
        }
    }

    public synchronized void deactivateAccountAccess() {
        if (accountAccessComponent != null) {
            accountAccessComponent.deactivate();
        }
    }

    public synchronized void deactivateAttendance() {
        if (attendanceComponent != null) {
            attendanceComponent.deactivate();
        }
    }

    public synchronized void activateAgent(ShellAccessState access) {
        if (agentComponent != null) {
            agentComponent.activate(access);
        }
    }

    public synchronized void deactivateAgent() {
        if (agentComponent != null) {
            agentComponent.deactivate();
        }
    }

    public synchronized void activateNotifications(ShellAccessState access) {
        if (notificationsComponent != null) {
            notificationsComponent.activate(access);
        }
    }

    public synchronized void deactivateNotifications() {
        if (notificationsComponent != null) {
            notificationsComponent.deactivate(true);
        }
    }

    @Override
    public synchronized Optional<CatalogController> catalogController() {
        return catalogSession.map(CatalogSession::controller);
    }

    @Override
    public synchronized Optional<CatalogAdministrationFeatureRuntime>
            catalogAdministrationRuntime() {
        return catalogAdministrationSession.map(
                CatalogAdministrationSession::runtime);
    }

    @Override
    public synchronized Optional<CustomerFeatureRuntime> customerRuntime() {
        return customerSession.map(CustomerSession::runtime);
    }

    @Override
    public synchronized Optional<MaterialQuoteFeatureRuntime> materialQuoteRuntime() {
        return materialQuoteSession.map(MaterialQuoteSession::runtime);
    }

    @Override
    public synchronized Optional<LaborQuoteFeatureRuntime> laborQuoteRuntime() {
        return laborQuoteSession.map(LaborQuoteSession::runtime);
    }

    @Override
    public synchronized Optional<OrderFeatureRuntime> orderRuntime() {
        return orderSession.map(OrderSession::runtime);
    }

    @Override
    public synchronized Optional<DeliveryFeatureRuntime>
            deliveryRuntime() {
        return deliverySession.map(DeliverySession::runtime);
    }

    @Override
    public synchronized Optional<CommissionFeatureRuntime> commissionRuntime() {
        return commissionSession.map(CommissionSession::runtime);
    }

    @Override
    public synchronized Optional<DashboardFeatureRuntime> dashboardRuntime() {
        return dashboardSession.map(DashboardSession::runtime);
    }

    @Override
    public synchronized Optional<TeamFeatureRuntime> teamRuntime() {
        return teamSession.map(TeamSession::runtime);
    }

    @Override
    public synchronized Optional<PainterAdministrationFeatureRuntime>
            painterAdministrationRuntime() {
        return painterAdministrationSession.map(
                PainterAdministrationSession::runtime);
    }

    @Override
    public synchronized Optional<UserAdministrationFeatureRuntime>
            userAdministrationRuntime() {
        return userAdministrationSession.map(UserAdministrationSession::runtime);
    }

    @Override
    public synchronized Optional<OrganizationAdministrationFeatureRuntime>
            organizationAdministrationRuntime() {
        return organizationAdministrationSession.map(
                OrganizationAdministrationSession::runtime);
    }

    @Override
    public synchronized Optional<SystemConfigurationFeatureRuntime>
            systemConfigurationRuntime() {
        return systemConfigurationSession.map(SystemConfigurationSession::runtime);
    }

    @Override
    public synchronized Optional<WhatsAppAdministrationFeatureRuntime>
            whatsAppAdministrationRuntime() {
        return whatsAppAdministrationSession.map(
                WhatsAppAdministrationSession::runtime);
    }

    @Override
    public synchronized Optional<AuditFeatureRuntime> auditRuntime() {
        return auditSession.map(AuditSession::runtime);
    }

    @Override
    public synchronized Optional<FinanceFeatureRuntime> financeRuntime(
            FinanceRoute route) {
        if (route == null) {
            return Optional.empty();
        }
        if (route.experience()
                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.PERSONAL) {
            return financeSession.map(FinanceSession::runtime);
        }
        Optional<CorporateFinanceDirectorySession> directory =
                corporateFinanceDirectorySession;
        if (directory.isEmpty()
                || (!directory.orElseThrow().scope().globalAccess()
                        && route.organizationId().isEmpty())
                || protectedApi == null
                || financeWorker == null) {
            return Optional.empty();
        }
        CorporateFinanceAccountScope scope =
                new CorporateFinanceAccountScope(
                        directory.orElseThrow().scope().userId(),
                        directory.orElseThrow()
                                .scope()
                                .authorizationRevision(),
                        route.organizationId(),
                        directory.orElseThrow().scope().globalAccess());
        if (corporateFinanceEntrySession
                .filter(session -> session.scope().equals(scope)
                        && session.runtime().route().equals(route))
                .isEmpty()) {
            Optional<CorporateFinanceEntrySession> previous =
                    corporateFinanceEntrySession;
            previous.ifPresent(session -> session.repository().close());
            RemoteCorporateFinanceRepository repository =
                    new RemoteCorporateFinanceRepository(scope, protectedApi);
            corporateFinanceEntrySession = Optional.of(
                    new CorporateFinanceEntrySession(
                            scope,
                            repository,
                            new FinanceFeatureRuntime(
                                    repository,
                                    financeWorker,
                                    route)));
        }
        return corporateFinanceEntrySession.map(
                CorporateFinanceEntrySession::runtime);
    }

    @Override
    public synchronized Optional<CorporateFinanceFeatureRuntime>
            corporateFinanceRuntime() {
        return corporateFinanceDirectorySession.map(
                CorporateFinanceDirectorySession::runtime);
    }

    @Override
    public synchronized Optional<AppointmentFeatureRuntime> appointmentRuntime() {
        return appointmentSession.map(AppointmentSession::runtime);
    }

    @Override
    public synchronized Optional<AttendanceFeatureRuntime>
            attendanceRuntime() {
        return attendanceComponent == null
                ? Optional.empty()
                : attendanceComponent.runtime();
    }

    @Override
    public synchronized Optional<AgentFeatureRuntime> agentRuntime() {
        return agentComponent == null
                ? Optional.empty()
                : agentComponent.runtime();
    }

    @Override
    public synchronized Optional<AccountAccessFeatureRuntime>
            accountAccessRuntime() {
        return accountAccessComponent == null
                ? Optional.empty()
                : accountAccessComponent.runtime();
    }

    @Override
    public synchronized Optional<ManagedAccountAccessFeatureRuntime>
            managedAccountAccessRuntime() {
        if (protectedApi == null
                || userAdministrationWorker == null
                || googleIdTokenRequester == null) {
            return Optional.empty();
        }
        return userAdministrationSession
                .filter(session -> session.runtime()
                        .securityNavigator()
                        .isPresent())
                .map(session -> new ManagedAccountAccessFeatureRuntime(
                        targetUserId -> new RemoteManagedAccountAccessReader(
                                new ManagedAccountAccessScope(
                                        session.scope().userId(),
                                        targetUserId,
                                        session.scope().authorizationRevision()),
                                protectedApi),
                        targetUserId ->
                                new RemoteManagedAccountRevocationRepository(
                                        new ManagedAccountAccessScope(
                                                session.scope().userId(),
                                                targetUserId,
                                                session.scope()
                                                        .authorizationRevision()),
                                        protectedApi),
                        userAdministrationWorker,
                        googleIdTokenRequester,
                        this::rejectAccountSession));
    }

    @Override
    public synchronized Optional<NotificationFeatureRuntime>
            notificationRuntime() {
        return notificationsComponent == null
                ? Optional.empty()
                : notificationsComponent.runtime();
    }

    public synchronized void activateWorkforceLocation(ShellAccessState access) {
        workforceLocationOrganizationId = BuildConfig.IS_WORKFORCE
                && access.selectedOrganization().isPresent()
                        ? OptionalLong.of(
                                access.selectedOrganization().orElseThrow().id())
                        : OptionalLong.empty();
    }

    public synchronized void deactivateWorkforceLocation() {
        workforceLocationOrganizationId = OptionalLong.empty();
    }

    @Override
    public synchronized Optional<MobileApi> workforceLocationApi() {
        return BuildConfig.IS_WORKFORCE
                ? Optional.ofNullable(protectedApi)
                : Optional.empty();
    }

    @Override
    public synchronized OptionalLong workforceLocationOrganizationId() {
        return workforceLocationOrganizationId;
    }

    @Override
    public String workforceLocationAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public boolean isWorkforceLocationBuild() {
        return BuildConfig.IS_WORKFORCE;
    }

    @Override
    public void onTerminate() {
        if (workforceRecoveryStarted.compareAndSet(true, false)) {
            if (authController != null) {
                authController.unsubscribe(workforceAuthListener);
            }
            if (bootstrapController != null) {
                bootstrapController.unsubscribe(workforceBootstrapListener);
            }
        }
        deactivateCatalog(false);
        deactivateCatalogAdministration();
        deactivateCustomers();
        deactivateMaterialQuotes();
        deactivateLaborQuotes();
        deactivateOrders();
        deactivateDeliveries();
        deactivateDashboard();
        deactivateTeam();
        deactivatePainterAdministration();
        deactivateUserAdministration();
        deactivateOrganizationAdministration();
        deactivateSystemConfiguration();
        deactivateWhatsAppAdministration();
        deactivateAudit();
        deactivateCommissions();
        deactivateFinance();
        deactivateAppointments();
        if (accountAccessComponent != null) {
            accountAccessComponent.close();
        }
        if (attendanceComponent != null) {
            attendanceComponent.close();
        }
        if (agentComponent != null) {
            agentComponent.close();
        }
        if (notificationsComponent != null) {
            notificationsComponent.close();
        }
        if (catalogDataEnvironment != null) {
            catalogDataEnvironment.close();
        }
        if (authWorker != null) {
            authWorker.shutdownNow();
        }
        if (catalogWorker != null) {
            catalogWorker.shutdownNow();
        }
        if (catalogAdministrationWorker != null) {
            catalogAdministrationWorker.shutdownNow();
        }
        if (customerWorker != null) {
            customerWorker.shutdownNow();
        }
        if (quoteWorker != null) {
            quoteWorker.shutdownNow();
        }
        if (laborQuoteWorker != null) {
            laborQuoteWorker.shutdownNow();
        }
        if (orderWorker != null) {
            orderWorker.shutdownNow();
        }
        if (deliveryWorker != null) {
            deliveryWorker.shutdownNow();
        }
        if (dashboardWorker != null) {
            dashboardWorker.shutdownNow();
        }
        if (teamWorker != null) {
            teamWorker.shutdownNow();
        }
        if (painterAdministrationWorker != null) {
            painterAdministrationWorker.shutdownNow();
        }
        if (userAdministrationWorker != null) {
            userAdministrationWorker.shutdownNow();
        }
        if (organizationAdministrationWorker != null) {
            organizationAdministrationWorker.shutdownNow();
        }
        if (systemConfigurationWorker != null) {
            systemConfigurationWorker.shutdownNow();
        }
        if (whatsAppAdministrationWorker != null) {
            whatsAppAdministrationWorker.shutdownNow();
        }
        if (auditWorker != null) {
            auditWorker.shutdownNow();
        }
        if (commissionWorker != null) {
            commissionWorker.shutdownNow();
        }
        if (financeWorker != null) {
            financeWorker.shutdownNow();
        }
        if (appointmentWorker != null) {
            appointmentWorker.shutdownNow();
        }
        super.onTerminate();
    }

    private synchronized CatalogDataEnvironment catalogDataEnvironment() {
        if (catalogDataEnvironment == null) {
            catalogDataEnvironment = CatalogDataEnvironment.create(this);
        }
        return catalogDataEnvironment;
    }

    private static Set<MobileArea> implementedAreas() {
        Set<MobileArea> result = EnumSet.copyOf(BASE_IMPLEMENTED_AREAS);
        if (BuildConfig.IS_WORKFORCE) {
            result.add(MobileArea.WORKFORCE_LOCATION);
        }
        return Set.copyOf(result);
    }

    private static ExpectedBootstrapIdentity expectedBootstrapIdentity(
            AuthenticatedSession session) {
        return new ExpectedBootstrapIdentity(
                session.user().id(),
                session.session().id(),
                session.session().deviceId());
    }

    private void rejectAccountSession() {
        AuthController current = authController;
        if (current != null) {
            current.rejectSession();
        }
    }

    private static void clearCatalogAccount(CachedCatalogRepository repository) {
        try {
            repository.clearAccount();
        } catch (CatalogException ignored) {
            // Cache remains scoped and inaccessible; a later cleanup can retry safely.
        }
    }

    private static boolean sameScope(
            CatalogAccountScope first,
            CatalogAccountScope second) {
        return first.accountKey().equals(second.accountKey())
                && first.authorizationRevision().equals(second.authorizationRevision());
    }

    private record CatalogSession(
            CatalogAccountScope scope,
            CachedCatalogRepository repository,
            CatalogController controller) {
    }

    private record CatalogAdministrationSession(
            CatalogAdministrationAccountScope scope,
            RemoteCatalogAdministrationRepository repository,
            RemoteCatalogImportRepository importRepository,
            CatalogAdministrationFeatureRuntime runtime) {
    }

    private record CustomerSession(
            CustomerAccountScope scope,
            RemoteCustomerRepository repository,
            CustomerFeatureRuntime runtime) {
    }

    private record MaterialQuoteSession(
            MaterialQuoteAccountScope scope,
            RemoteMaterialQuoteRepository repository,
            MaterialQuoteFeatureRuntime runtime) {
    }

    private record LaborQuoteSession(
            LaborQuoteAccountScope scope,
            RemoteLaborQuoteRepository repository,
            LaborQuoteFeatureRuntime runtime) {
    }

    private record OrderSession(
            OrderAccountScope scope,
            RemoteOrderRepository repository,
            OrderFeatureRuntime runtime) {
    }

    private record DeliverySession(
            DeliveryAccountScope scope,
            RemoteDeliveryRepository repository,
            Optional<RemoteDeliveryManagementRepository>
                    managementRepository,
            DeliveryFeatureRuntime runtime) {
    }

    private record DashboardSession(
            DashboardAccountScope scope,
            RemoteDashboardRepository repository,
            DashboardFeatureRuntime runtime) {
    }

    private record TeamSession(
            TeamAccountScope scope,
            RemoteTeamRepository repository,
            TeamFeatureRuntime runtime) {
    }

    private record PainterAdministrationSession(
            PainterAdministrationAccountScope scope,
            RemotePainterAdministrationRepository repository,
            PainterAdministrationFeatureRuntime runtime) {
    }

    private record UserAdministrationSession(
            UserAdministrationAccountScope scope,
            RemoteUserAdministrationRepository repository,
            UserAdministrationFeatureRuntime runtime) {
    }

    private record OrganizationAdministrationSession(
            OrganizationAdministrationAccountScope scope,
            RemoteOrganizationAdministrationRepository repository,
            OrganizationAdministrationFeatureRuntime runtime) {
    }

    private record SystemConfigurationSession(
            SystemConfigurationAccountScope scope,
            RemoteSystemConfigurationRepository repository,
            SystemConfigurationController controller,
            SystemConfigurationFeatureRuntime runtime) {
    }

    private record WhatsAppAdministrationSession(
            WhatsAppAdministrationAccountScope scope,
            RemoteWhatsAppAdministrationRepository repository,
            WhatsAppAdministrationFeatureRuntime runtime) {
    }

    private record AuditSession(
            AuditAccountScope scope,
            RemoteAuditRepository repository,
            Optional<RemoteAgentReplayRepository> replayRepository,
            AuditFeatureRuntime runtime) {
    }

    private record CommissionSession(
            CommissionAccountScope scope,
            RemoteCommissionRepository repository,
            CommissionFeatureRuntime runtime) {
    }

    private record FinanceSession(
            FinanceAccountScope scope,
            RemoteFinanceRepository repository,
            FinanceFeatureRuntime runtime) {
    }

    private record CorporateFinanceDirectorySession(
            CorporateFinanceAccountScope scope,
            RemoteCorporateFinanceOrganizationRepository repository,
            CorporateFinanceFeatureRuntime runtime) {
    }

    private record CorporateFinanceEntrySession(
            CorporateFinanceAccountScope scope,
            RemoteCorporateFinanceRepository repository,
            FinanceFeatureRuntime runtime) {
    }

    private record AppointmentSession(
            AppointmentAccountScope scope,
            RemoteAppointmentRepository repository,
            AppointmentFeatureRuntime runtime) {
    }
}
