package br.com.tresvtintas.mobile.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import br.com.tresvtintas.mobile.app.databinding.ActivityMainBinding;
import br.com.tresvtintas.mobile.core.auth.AuthController;
import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.AuthState;
import br.com.tresvtintas.mobile.core.auth.AuthStateListener;
import br.com.tresvtintas.mobile.core.auth.AuthenticatedSession;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapController;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapFailureKind;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapState;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapStateListener;
import br.com.tresvtintas.mobile.core.bootstrap.ExpectedBootstrapIdentity;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.core.notifications.NotificationRoute;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessActivity;
import br.com.tresvtintas.mobile.feature.agent.AgentConversationListActivity;
import br.com.tresvtintas.mobile.feature.catalog.CatalogActivity;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceListActivity;
import br.com.tresvtintas.mobile.feature.appointment.AppointmentListActivity;
import br.com.tresvtintas.mobile.feature.audit.AuditActivity;
import br.com.tresvtintas.mobile.feature.commission.CommissionListActivity;
import br.com.tresvtintas.mobile.feature.corporatefinance.CorporateFinanceOrganizationActivity;
import br.com.tresvtintas.mobile.feature.customer.CustomerListActivity;
import br.com.tresvtintas.mobile.feature.dashboard.DashboardActivity;
import br.com.tresvtintas.mobile.feature.team.TeamActivity;
import br.com.tresvtintas.mobile.feature.painteradmin.PainterAdministrationListActivity;
import br.com.tresvtintas.mobile.feature.useradmin.UserAdministrationListActivity;
import br.com.tresvtintas.mobile.feature.organizationadmin.OrganizationAdministrationActivity;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryListActivity;
import br.com.tresvtintas.mobile.feature.finance.FinanceListActivity;
import br.com.tresvtintas.mobile.feature.finance.FinanceRoute;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteListActivity;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuoteListActivity;
import br.com.tresvtintas.mobile.feature.order.OrderListActivity;
import br.com.tresvtintas.mobile.feature.notifications.NotificationSettingsActivity;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellCoordinator;
import br.com.tresvtintas.mobile.feature.shell.ShellScopeKind;
import br.com.tresvtintas.mobile.feature.systemconfiguration.SystemConfigurationActivity;
import br.com.tresvtintas.mobile.feature.whatsappadmin.WhatsAppAdministrationActivity;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Root shell coordinator. Business journeys remain isolated in feature modules and are exposed only
 * after both implementation readiness and server authorization.
 */
public final class MainActivity extends AppCompatActivity
        implements MainShellController.Listener {
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
    private final AuthStateListener authListener = this::renderAuth;
    private final BootstrapStateListener bootstrapListener = this::renderBootstrap;
    private final ShellCoordinator shellCoordinator = new ShellCoordinator();
    private ActivityMainBinding binding;
    private MainScreenRenderer renderer;
    private MainShellController shellController;
    private ThreeVTintasApplication application;
    private AuthController authController;
    private BootstrapController bootstrapController;
    private Optional<AuthenticatedSession> authenticatedSession = Optional.empty();
    private Optional<ShellAccessState> shellAccess = Optional.empty();
    private Optional<NotificationRoute> pendingNotificationRoute =
            Optional.empty();
    private ScreenAction primaryAction = ScreenAction.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        renderer = new MainScreenRenderer(binding);
        shellController = new MainShellController(binding, this);
        setContentView(binding.getRoot());
        applySystemBarInsets(binding.getRoot());

        application = (ThreeVTintasApplication) getApplication();
        authController = application.authController().orElse(null);
        bootstrapController = application.bootstrapController().orElse(null);
        renderer.environment(BuildConfig.ENVIRONMENT);
        binding.primaryAction.setOnClickListener(ignored -> handlePrimaryAction());
        binding.logoutAction.setOnClickListener(ignored -> logout());
        captureNotificationRoute(getIntent());
        if (authController == null || bootstrapController == null) {
            AuthPresentation startupPresentation =
                    !BuildConfig.MOBILE_API_CONFIGURED
                            ? AuthPresentation.configurationRequired()
                            : application.authStartupFailure()
                                    .map(AuthPresentation::startupFailure)
                                    .orElseGet(() -> AuthPresentation.startupFailure(
                                            AuthFailureKind.PROTOCOL));
            primaryAction = renderer.auth(startupPresentation, Optional.empty());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authController != null && bootstrapController != null) {
            bootstrapController.subscribe(bootstrapListener);
            authController.subscribe(authListener);
            application.restoreAuthenticationOnce();
        }
    }

    @Override
    protected void onStop() {
        if (authController != null && bootstrapController != null) {
            authController.unsubscribe(authListener);
            bootstrapController.unsubscribe(bootstrapListener);
        }
        super.onStop();
    }

    private void renderAuth(AuthState state) {
        primaryAction = renderer.auth(AuthPresentation.from(state), state.session());
        renderer.requestId(state.requestId());
        if (state.phase() == AuthState.Phase.AUTHENTICATED) {
            AuthenticatedSession session = state.session().orElseThrow();
            authenticatedSession = Optional.of(session);
            bootstrapController.load(expectedIdentity(session));
        } else {
            authenticatedSession = Optional.empty();
            shellAccess = Optional.empty();
            shellCoordinator.clear();
            application.deactivateCatalog(
                    state.phase() == AuthState.Phase.SIGNED_OUT
                            || state.phase() == AuthState.Phase.SIGNING_OUT);
            application.deactivateCatalogAdministration();
            application.deactivateCustomers();
            application.deactivateMaterialQuotes();
            application.deactivateLaborQuotes();
            application.deactivateOrders();
            application.deactivateDeliveries();
            application.deactivateDashboard();
            application.deactivateTeam();
            application.deactivatePainterAdministration();
            application.deactivateUserAdministration();
            application.deactivateOrganizationAdministration();
            application.deactivateAudit();
            application.deactivateCommissions();
            application.deactivateFinance();
            application.deactivateAppointments();
            application.deactivateAttendance();
            application.deactivateAgent();
            application.deactivateAccountAccess();
            application.deactivateNotifications();
            application.deactivateSystemConfiguration();
            application.deactivateWhatsAppAdministration();
            shellController.clear();
            renderer.hideShell();
            if (bootstrapController.currentState().phase() != BootstrapState.Phase.EMPTY) {
                bootstrapController.clear();
            }
        }
        if (state.phase() == AuthState.Phase.SIGNED_OUT
                && state.failure().isEmpty()
                && application.shouldAttemptAuthorizedPrompt()) {
            authController.signInAuthorized(this);
        }
    }

    private void renderBootstrap(BootstrapState state) {
        if (state.phase() == BootstrapState.Phase.EMPTY) {
            return;
        }
        if (state.phase() == BootstrapState.Phase.READY) {
            ShellAccessState access = shellCoordinator.apply(state.snapshot().orElseThrow());
            shellAccess = Optional.of(access);
            Set<MobileArea> enabledAreas = enabledAreas(access);
            applyFeatureAccess(access, enabledAreas);
            primaryAction = renderer.shell(access);
            shellController.render(enabledAreas);
            renderer.requestId(Optional.empty());
            openPendingNotification(enabledAreas);
            return;
        }
        application.deactivateCatalog(false);
        application.deactivateCatalogAdministration();
        application.deactivateCustomers();
        application.deactivateMaterialQuotes();
        application.deactivateLaborQuotes();
        application.deactivateOrders();
        application.deactivateDeliveries();
        application.deactivateDashboard();
        application.deactivateTeam();
        application.deactivatePainterAdministration();
        application.deactivateUserAdministration();
        application.deactivateOrganizationAdministration();
        application.deactivateAudit();
        application.deactivateCommissions();
        application.deactivateFinance();
        application.deactivateAppointments();
        application.deactivateAttendance();
        application.deactivateAgent();
        application.deactivateAccountAccess();
        application.deactivateNotifications();
        application.deactivateSystemConfiguration();
        application.deactivateWhatsAppAdministration();
        shellController.clear();
        if (state.failure().orElse(null) == BootstrapFailureKind.AUTH_REJECTED) {
            authController.rejectSession();
            return;
        }
        primaryAction = renderer.bootstrap(BootstrapPresentation.from(state));
        renderer.requestId(state.requestId());
    }

    private void handlePrimaryAction() {
        switch (primaryAction) {
            case SIGN_IN -> authController.signInExplicit(this);
            case RETRY_AUTH -> authController.restore();
            case RETRY_BOOTSTRAP -> refreshBootstrap();
            case SELECT_ORGANIZATION -> showOrganizationPicker();
            case NONE -> {
                // No operation is deliberately exposed for this state.
            }
            default -> throw new IllegalStateException("Unsupported primary screen action.");
        }
    }

    @Override
    public void logout() {
        if (authController != null) {
            authController.logout();
        }
    }

    @Override
    public void refreshAccess() {
        refreshBootstrap();
    }

    private void refreshBootstrap() {
        if (bootstrapController != null) {
            authenticatedSession.ifPresent(session ->
                    bootstrapController.refresh(expectedIdentity(session)));
        }
    }

    @Override
    public void chooseOrganization() {
        showOrganizationPicker();
    }

    private void showOrganizationPicker() {
        showOrganizationPicker(() -> { });
    }

    private void showOrganizationPicker(Runnable afterSelection) {
        if (afterSelection == null) {
            return;
        }
        shellAccess.ifPresent(access -> {
            if (access.scopeKind() == ShellScopeKind.ORGANIZATION_ASSIGNMENT_REQUIRED) {
                showFeatureUnavailable(R.string.shell_assignment_required_message);
                return;
            }
            if (access.scopeKind() == ShellScopeKind.ORGANIZATION_LIST_INCOMPLETE) {
                showFeatureUnavailable(R.string.shell_incomplete_store_list_message);
                return;
            }
            if (access.bootstrap().authorization().organizationAccessMode()
                    == OrganizationAccessMode.ALL) {
                application.corporateFinanceRuntime().ifPresentOrElse(runtime ->
                        GlobalOrganizationPicker.show(
                                this,
                                runtime.organizationRepository(),
                                runtime.workerExecutor(),
                                ContextCompat.getMainExecutor(this),
                                organizations -> {
                                    shellCoordinator.setGlobalOrganizations(organizations);
                                    OrganizationPicker.show(
                                            this,
                                            organizations,
                                            organizationId -> selectOrganization(
                                                    organizationId,
                                                    afterSelection));
                                }),
                        () -> showFeatureUnavailable(
                                R.string.shell_global_store_error_message));
                return;
            }
            OrganizationPicker.show(this, access, organizationId ->
                    selectOrganization(organizationId, afterSelection));
        });
    }

    private void selectOrganization(
            long organizationId,
            Runnable afterSelection) {
        try {
            ShellAccessState selected =
                    shellCoordinator.selectOrganization(organizationId);
            shellAccess = Optional.of(selected);
            Set<MobileArea> enabledAreas = enabledAreas(selected);
            applyFeatureAccess(selected, enabledAreas);
            primaryAction = renderer.shell(selected);
            shellController.render(enabledAreas);
            if (afterSelection != null) {
                afterSelection.run();
            }
        } catch (IllegalArgumentException | IllegalStateException failure) {
            showFeatureUnavailable(R.string.shell_global_store_error_message);
        }
    }

    @Override
    public void open(MobileArea area) {
        switch (area) {
            case ACCOUNT_SECURITY -> openAccountSecurity();
            case DASHBOARD -> openDashboard();
            case TEAM -> openTeam();
            case PAINTERS_AND_APPROVALS -> openPainterAdministration();
            case TEAM_AND_USERS -> openUserAdministration();
            case ORGANIZATION_ADMINISTRATION -> openOrganizationAdministration();
            case AUDIT -> openAudit();
            case CATALOG -> openCatalog();
            case CUSTOMERS -> openCustomers();
            case MATERIAL_QUOTES -> openMaterialQuotes();
            case LABOR_QUOTES -> openLaborQuotes();
            case ORDERS -> openOrders();
            case DELIVERIES -> openDeliveries();
            case COMMISSIONS -> openCommissions(false);
            case COMMISSION_TEAM -> openCommissions(true);
            case PERSONAL_FINANCE -> openFinance();
            case CORPORATE_FINANCE -> openCorporateFinance();
            case AGENDA, TEAM_AGENDA, GLOBAL_AGENDA -> openAppointments();
            case CUSTOMER_SERVICE -> openAttendance();
            case PERSONAL_AI_AGENT -> openAgent();
            case NOTIFICATIONS -> openNotifications();
            case WORKFORCE_LOCATION -> openWorkforceLocation();
            case SETTINGS_AND_PROFILE -> openSystemConfiguration();
            case WHATSAPP_INTEGRATIONS -> openWhatsAppAdministration();
            default -> throw new IllegalStateException(
                    "Unsupported shell area.");
        }
    }

    private void applyFeatureAccess(
            ShellAccessState access,
            Set<MobileArea> enabledAreas) {
        if (enabledAreas.contains(MobileArea.ACCOUNT_SECURITY)
                && authenticatedSession.isPresent()) {
            application.activateAccountAccess(
                    access,
                    authenticatedSession.orElseThrow());
        } else {
            application.deactivateAccountAccess();
        }
        if (enabledAreas.contains(MobileArea.DASHBOARD)) {
            application.activateDashboard(access);
        } else {
            application.deactivateDashboard();
        }
        if (enabledAreas.contains(MobileArea.TEAM)) {
            application.activateTeam(access);
        } else {
            application.deactivateTeam();
        }
        if (enabledAreas.contains(MobileArea.PAINTERS_AND_APPROVALS)) {
            application.activatePainterAdministration(access);
        } else {
            application.deactivatePainterAdministration();
        }
        if (enabledAreas.contains(MobileArea.TEAM_AND_USERS)) {
            application.activateUserAdministration(access);
        } else {
            application.deactivateUserAdministration();
        }
        if (enabledAreas.contains(MobileArea.ORGANIZATION_ADMINISTRATION)) {
            application.activateOrganizationAdministration(access);
        } else {
            application.deactivateOrganizationAdministration();
        }
        if (enabledAreas.contains(MobileArea.AUDIT)) {
            application.activateAudit(access);
        } else {
            application.deactivateAudit();
        }
        if (enabledAreas.contains(MobileArea.CATALOG)) {
            application.activateCatalog(access);
            application.activateCatalogAdministration(access);
        } else {
            application.deactivateCatalog(false);
            application.deactivateCatalogAdministration();
        }
        if (enabledAreas.contains(MobileArea.CUSTOMERS)) {
            application.activateCustomers(access);
        } else {
            application.deactivateCustomers();
        }
        if (enabledAreas.contains(MobileArea.ORDERS)) {
            application.activateOrders(access);
        } else {
            application.deactivateOrders();
        }
        if (enabledAreas.contains(MobileArea.DELIVERIES)) {
            application.activateDeliveries(access);
        } else {
            application.deactivateDeliveries();
        }
        if (enabledAreas.contains(MobileArea.MATERIAL_QUOTES)) {
            application.activateMaterialQuotes(access);
        } else {
            application.deactivateMaterialQuotes();
        }
        if (enabledAreas.contains(MobileArea.LABOR_QUOTES)) {
            application.activateLaborQuotes(access);
        } else {
            application.deactivateLaborQuotes();
        }
        if (enabledAreas.contains(MobileArea.COMMISSIONS)
                || enabledAreas.contains(MobileArea.COMMISSION_TEAM)) {
            application.activateCommissions(access);
        } else {
            application.deactivateCommissions();
        }
        if (enabledAreas.contains(MobileArea.PERSONAL_FINANCE)
                || enabledAreas.contains(MobileArea.CORPORATE_FINANCE)) {
            application.activateFinance(access);
        } else {
            application.deactivateFinance();
        }
        if (hasAppointmentArea(enabledAreas)) {
            application.activateAppointments(access);
        } else {
            application.deactivateAppointments();
        }
        if (enabledAreas.contains(MobileArea.CUSTOMER_SERVICE)) {
            application.activateAttendance(access);
        } else {
            application.deactivateAttendance();
        }
        if (enabledAreas.contains(MobileArea.PERSONAL_AI_AGENT)) {
            application.activateAgent(access);
        } else {
            application.deactivateAgent();
        }
        if (enabledAreas.contains(MobileArea.NOTIFICATIONS)) {
            application.activateNotifications(access);
        } else {
            application.deactivateNotifications();
        }
        if (enabledAreas.contains(MobileArea.WORKFORCE_LOCATION)) {
            application.activateWorkforceLocation(access);
        } else {
            application.deactivateWorkforceLocation();
        }
        if (enabledAreas.contains(MobileArea.SETTINGS_AND_PROFILE)) {
            application.activateSystemConfiguration(access);
        } else {
            application.deactivateSystemConfiguration();
        }
        if (enabledAreas.contains(MobileArea.WHATSAPP_INTEGRATIONS)) {
            application.activateWhatsAppAdministration(access);
        } else {
            application.deactivateWhatsAppAdministration();
        }
    }

    private void openCatalog() {
        if (application.catalogController().isPresent()) {
            startActivity(new Intent(this, CatalogActivity.class));
            return;
        }
        openAfterOrganizationSelection(
                this::openCatalog,
                R.string.shell_catalog_store_required_message);
    }

    private void openDashboard() {
        if (application.dashboardRuntime().isPresent()) {
            startActivity(new Intent(this, DashboardActivity.class));
        }
    }

    private void openTeam() {
        if (application.teamRuntime().isPresent()) {
            startActivity(new Intent(this, TeamActivity.class));
        }
    }

    private void openPainterAdministration() {
        if (application.painterAdministrationRuntime().isPresent()) {
            startActivity(PainterAdministrationListActivity.intent(this));
        }
    }

    private void openUserAdministration() {
        if (application.userAdministrationRuntime().isPresent()) {
            startActivity(UserAdministrationListActivity.intent(this));
        }
    }

    private void openOrganizationAdministration() {
        if (application.organizationAdministrationRuntime().isPresent()) {
            startActivity(OrganizationAdministrationActivity.intent(this));
        }
    }

    private void openAudit() {
        if (application.auditRuntime().isPresent()) {
            startActivity(AuditActivity.intent(this));
        }
    }

    private void openCustomers() {
        if (application.customerRuntime().isPresent()) {
            startActivity(new Intent(this, CustomerListActivity.class));
        }
    }

    private void openMaterialQuotes() {
        if (application.materialQuoteRuntime().isPresent()) {
            startActivity(new Intent(this, MaterialQuoteListActivity.class));
            return;
        }
        openAfterOrganizationSelection(
                this::openMaterialQuotes,
                R.string.shell_quotes_store_required_message);
    }

    private void openAfterOrganizationSelection(
            Runnable open,
            int unavailableMessage) {
        Optional<ShellAccessState> currentAccess = shellAccess;
        if (currentAccess.isEmpty()) {
            showFeatureUnavailable(unavailableMessage);
            return;
        }
        ShellAccessState access = currentAccess.orElseThrow();
        if (!access.isOperational()) {
            showFeatureUnavailable(unavailableMessage);
            return;
        }
        if (access.selectedOrganization().isEmpty()) {
            showOrganizationPicker(open);
            return;
        }
        showFeatureUnavailable(unavailableMessage);
    }

    private void showFeatureUnavailable(int message) {
        android.widget.Toast.makeText(
                this,
                message,
                android.widget.Toast.LENGTH_LONG).show();
    }

    private void openLaborQuotes() {
        if (application.laborQuoteRuntime().isPresent()) {
            startActivity(new Intent(this, LaborQuoteListActivity.class));
        }
    }

    private void openOrders() {
        if (application.orderRuntime().isPresent()) {
            startActivity(new Intent(this, OrderListActivity.class));
        }
    }

    private void openDeliveries() {
        if (application.deliveryRuntime().isPresent()) {
            startActivity(new Intent(
                    this,
                    DeliveryListActivity.class));
        }
    }

    private void openCommissions(boolean team) {
        if (application.commissionRuntime().isPresent()) {
            startActivity(CommissionListActivity.intent(
                    this,
                    team
                            ? CommissionScope.TEAM
                            : CommissionScope.SELF));
        }
    }

    private void openFinance() {
        if (application.financeRuntime(FinanceRoute.personal()).isPresent()) {
            startActivity(FinanceListActivity.intent(this));
        }
    }

    private void openCorporateFinance() {
        if (application.corporateFinanceRuntime().isPresent()) {
            startActivity(CorporateFinanceOrganizationActivity.intent(this));
        }
    }

    private void openAppointments() {
        if (application.appointmentRuntime().isPresent()) {
            startActivity(AppointmentListActivity.intent(this));
        }
    }

    private void openAttendance() {
        if (application.attendanceRuntime().isPresent()) {
            startActivity(AttendanceListActivity.intent(this));
        }
    }

    private void openAgent() {
        if (application.agentRuntime().isPresent()) {
            startActivity(AgentConversationListActivity.intent(this));
        }
    }

    private void openAccountSecurity() {
        startActivity(AccountAccessActivity.intent(this));
    }

    private void openNotifications() {
        if (application.notificationRuntime().isPresent()) {
            startActivity(new Intent(this, NotificationSettingsActivity.class));
        }
    }

    private void openSystemConfiguration() {
        if (application.systemConfigurationRuntime().isPresent()) {
            startActivity(new Intent(this, SystemConfigurationActivity.class));
        }
    }

    private void openWhatsAppAdministration() {
        if (application.whatsAppAdministrationRuntime().isPresent()) {
            startActivity(WhatsAppAdministrationActivity.intent(this));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captureNotificationRoute(intent);
        shellAccess.ifPresent(access ->
                openPendingNotification(enabledAreas(access)));
    }

    private void captureNotificationRoute(Intent intent) {
        if (intent == null) {
            return;
        }
        String routeValue = intent.getStringExtra(
                NotificationRoute.INTENT_EXTRA);
        String eventId = intent.getStringExtra(
                NotificationRoute.EVENT_ID_EXTRA);
        if (routeValue == null || eventId == null) {
            return;
        }
        try {
            UUID.fromString(eventId);
            pendingNotificationRoute =
                    NotificationRoute.fromWireValue(routeValue);
        } catch (IllegalArgumentException ignored) {
            pendingNotificationRoute = Optional.empty();
        }
        intent.removeExtra(NotificationRoute.INTENT_EXTRA);
        intent.removeExtra(NotificationRoute.EVENT_ID_EXTRA);
    }

    private void openPendingNotification(Set<MobileArea> enabledAreas) {
        Optional<NotificationRoute> pending = pendingNotificationRoute;
        pendingNotificationRoute = Optional.empty();
        if (pending.isEmpty()) {
            return;
        }
        Optional<NotificationRoutePolicy.Destination> destination =
                NotificationRoutePolicy.destination(
                        pending.orElseThrow(),
                        enabledAreas);
        if (destination.isEmpty()) {
            return;
        }
        switch (destination.orElseThrow()) {
            case HOME -> {
                // The authorized shell is already visible.
            }
            case ATTENDANCE -> openAttendance();
            case ORDERS -> openOrders();
            case DELIVERIES -> openDeliveries();
            case MATERIAL_QUOTES -> openMaterialQuotes();
            case PERSONAL_COMMISSIONS -> openCommissions(false);
            case TEAM_COMMISSIONS -> openCommissions(true);
            case APPOINTMENTS -> openAppointments();
            case PERSONAL_FINANCE -> openFinance();
            case CORPORATE_FINANCE -> openCorporateFinance();
            case AGENT -> openAgent();
            case ACCOUNT_SECURITY -> openAccountSecurity();
            default -> throw new IllegalStateException(
                    "Unsupported notification destination.");
        }
    }

    private static boolean hasAppointmentArea(Set<MobileArea> enabledAreas) {
        return enabledAreas.contains(MobileArea.AGENDA)
                || enabledAreas.contains(MobileArea.TEAM_AGENDA)
                || enabledAreas.contains(MobileArea.GLOBAL_AGENDA);
    }

    private static Set<MobileArea> enabledAreas(ShellAccessState access) {
        return MainAreaPolicy.enabledAreas(
                access,
                IMPLEMENTED_AREAS);
    }

    private void openWorkforceLocation() {
        if (!BuildConfig.IS_WORKFORCE) {
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(
                this,
                "br.com.tresvtintas.mobile.feature.location.WorkforceLocationActivity");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            throw new IllegalStateException("Workforce location module is unavailable.", exception);
        }
    }

    private static Set<MobileArea> implementedAreas() {
        Set<MobileArea> result = EnumSet.copyOf(BASE_IMPLEMENTED_AREAS);
        if (BuildConfig.IS_WORKFORCE) {
            result.add(MobileArea.WORKFORCE_LOCATION);
        }
        return Set.copyOf(result);
    }

    private static ExpectedBootstrapIdentity expectedIdentity(
            AuthenticatedSession session) {
        return new ExpectedBootstrapIdentity(
                session.user().id(),
                session.session().id(),
                session.session().deviceId());
    }

    private static void applySystemBarInsets(View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return windowInsets;
        });
    }
}
