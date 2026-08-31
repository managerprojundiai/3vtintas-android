package br.com.tresvtintas.mobile.core.network.api;

import br.com.tresvtintas.mobile.core.network.RequiresAuthentication;
import br.com.tresvtintas.mobile.core.network.dto.AccountDevicePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountDeviceRevocationDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionRevocationDto;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountDevicePageDto;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountSessionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountRevocationDtos;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDecisionRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpChallengeRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpGrantResponse;
import br.com.tresvtintas.mobile.core.network.dto.AgentStepUpVerifyRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentCancelTurnRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentCreateConversationRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentCreateTurnRequest;
import br.com.tresvtintas.mobile.core.network.dto.AgentMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentTurnDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentReplayPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeRequest;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceAssigneePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceManagementRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceManagementResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMarkReadRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReadCursorResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReplyRequest;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReplyResponse;
import br.com.tresvtintas.mobile.core.network.dto.AuditPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentResponsiblePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentTransitionRequest;
import br.com.tresvtintas.mobile.core.network.dto.AppointmentUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;
import br.com.tresvtintas.mobile.core.network.dto.CatalogAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.dto.CatalogImportDtos;
import br.com.tresvtintas.mobile.core.network.dto.CatalogPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.CustomerDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CustomerPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.PricingContextDto;
import br.com.tresvtintas.mobile.core.network.dto.DashboardResponseDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryCompletionRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryPageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryRoutePageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryRoutePlanDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryStartRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementAssignmentRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementCompletionRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementDriversDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementOrganizationsDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementPageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementScheduleRequest;
import br.com.tresvtintas.mobile.core.network.dto.CommissionDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionApprovalRequest;
import br.com.tresvtintas.mobile.core.network.dto.CommissionCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.CommissionMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CommissionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CommissionPaymentRequest;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceOrganizationPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceSettlementRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginResponse;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuotePageDto;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteStatusMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.LaborQuoteUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreatePreviewRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteCreatePreviewResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuotePageDto;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteStatusMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdatePreviewRequest;
import br.com.tresvtintas.mobile.core.network.dto.MaterialQuoteUpdatePreviewResponse;
import br.com.tresvtintas.mobile.core.network.dto.MobileApiMeta;
import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import br.com.tresvtintas.mobile.core.network.dto.NotificationPreferencesDto;
import br.com.tresvtintas.mobile.core.network.dto.NotificationPreferencesUpdateRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderConversionRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderConversionResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrderMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrderPageDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderPaymentReceiptRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceSettlementRequest;
import br.com.tresvtintas.mobile.core.network.dto.PainterAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.dto.UserAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.dto.RefreshRequest;
import br.com.tresvtintas.mobile.core.network.dto.RefreshResponse;
import br.com.tresvtintas.mobile.core.network.dto.PushRegistrationRequest;
import br.com.tresvtintas.mobile.core.network.dto.PushRegistrationResponse;
import br.com.tresvtintas.mobile.core.network.dto.TeamResponseDto;
import br.com.tresvtintas.mobile.core.network.dto.TintSalesDtos;
import br.com.tresvtintas.mobile.core.network.dto.SystemConfigurationDtos;
import br.com.tresvtintas.mobile.core.network.dto.WhatsAppAdministrationDtos;
import okhttp3.ResponseBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.PUT;

/**
 * Authentication and compatibility surface of /api/mobile/v1.
 */
public interface MobileApi {
    String IDEMPOTENCY_HEADER = "Idempotency-Key";
    String QUOTE_ID_PATH_PARAMETER = "quoteId";
    String SEARCH_QUERY_PARAMETER = "search";
    String STATUS_QUERY_PARAMETER = "status";
    String TYPE_QUERY_PARAMETER = "type";
    String CURSOR_QUERY_PARAMETER = "cursor";
    String LIMIT_QUERY_PARAMETER = "limit";
    String VIEW_QUERY_PARAMETER = "view";
    String SCOPE_QUERY_PARAMETER = "scope";
    String FROM_QUERY_PARAMETER = "from";
    String TO_QUERY_PARAMETER = "to";
    String ORDER_ID_PATH_PARAMETER = "orderId";
    String COMMISSION_ID_PATH_PARAMETER = "commissionId";
    String FINANCE_ENTRY_ID_PATH_PARAMETER = "entryId";
    String ORGANIZATION_ID_QUERY_PARAMETER = "organizationId";
    String APPOINTMENT_ID_PATH_PARAMETER = "appointmentId";
    String DELIVERY_ID_PATH_PARAMETER = "deliveryId";
    String DELIVERY_ROUTE_KEY_PATH_PARAMETER = "routeKey";
    String ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER = "conversationId";
    String AGENT_CONVERSATION_ID_PATH_PARAMETER = "conversationId";
    String AGENT_TURN_ID_PATH_PARAMETER = "turnId";
    String AGENT_ACTION_ID_PATH_PARAMETER = "actionId";
    String SECURITY_ADMIN_ACTION_ID_PATH_PARAMETER = "actionId";
    String DEVICE_ID_PATH_PARAMETER = "deviceId";
    String WHATSAPP_CONNECTION_ID_PATH_PARAMETER = "connectionId";

    @RequiresAuthentication
    @GET("location/policy")
    Call<LocationDtos.PolicyResponse> locationPolicy(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId);

    @RequiresAuthentication
    @POST("location/consent")
    Call<LocationDtos.ConsentResponse> locationConsent(
            @Body LocationDtos.ConsentRequest request);

    @RequiresAuthentication
    @POST("location/heartbeat")
    Call<LocationDtos.HeartbeatResponse> locationHeartbeat(
            @Body LocationDtos.HeartbeatRequest request);

    @RequiresAuthentication
    @POST("location/batches")
    Call<LocationDtos.BatchResponse> locationBatch(
            @Body LocationDtos.BatchRequest request);

    @RequiresAuthentication
    @GET("location/history/self")
    Call<LocationDtos.HistoryResponse> locationHistory(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId,
            @Query(FROM_QUERY_PARAMETER) String from,
            @Query(TO_QUERY_PARAMETER) String to,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("system-configuration")
    Call<SystemConfigurationDtos.Configuration> systemConfiguration();

    @RequiresAuthentication
    @POST("system-configuration/actions")
    Call<SystemConfigurationDtos.Mutation> updateSystemConfiguration(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body SystemConfigurationDtos.UpdateRequest request);

    @RequiresAuthentication
    @GET("whatsapp-administration/stores")
    Call<WhatsAppAdministrationDtos.Snapshot> whatsAppAdministrationStores();

    @RequiresAuthentication
    @POST("whatsapp-administration/stores/{organizationId}/actions")
    Call<WhatsAppAdministrationDtos.ActionResult> configureMetaWhatsApp(
            @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body WhatsAppAdministrationDtos.ConfigureMetaRequest request);

    @RequiresAuthentication
    @POST("whatsapp-administration/stores/{organizationId}/actions")
    Call<WhatsAppAdministrationDtos.ActionResult> provisionEvolutionWhatsApp(
            @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body WhatsAppAdministrationDtos.ProvisionEvolutionRequest request);

    @RequiresAuthentication
    @POST("whatsapp-administration/stores/{organizationId}/actions")
    Call<WhatsAppAdministrationDtos.ActionResult> setWhatsAppMode(
            @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body WhatsAppAdministrationDtos.SetModeRequest request);

    @RequiresAuthentication
    @POST("whatsapp-administration/connections/{connectionId}/actions")
    Call<WhatsAppAdministrationDtos.ActionResult> refreshEvolutionWhatsApp(
            @Path(WHATSAPP_CONNECTION_ID_PATH_PARAMETER) long connectionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body WhatsAppAdministrationDtos.RefreshEvolutionRequest request);

    @RequiresAuthentication
    @POST("whatsapp-administration/connections/{connectionId}/qr")
    Call<WhatsAppAdministrationDtos.ActionResult> requestEvolutionWhatsAppQr(
            @Path(WHATSAPP_CONNECTION_ID_PATH_PARAMETER) long connectionId,
            @Body WhatsAppAdministrationDtos.QrRequest request);
    String SESSION_ID_PATH_PARAMETER = "sessionId";
    String PAINTER_ID_PATH_PARAMETER = "painterId";
    String ACCESS_REQUEST_ID_PATH_PARAMETER = "requestId";
    String USER_ID_PATH_PARAMETER = "userId";
    String ORGANIZATION_ID_PATH_PARAMETER = "organizationId";
    String ROLE_QUERY_PARAMETER = "role";
    String PRODUCT_ID_PATH_PARAMETER = "productId";
    String CATEGORY_ID_QUERY_PARAMETER = "categoryId";
    String ACTIVE_QUERY_PARAMETER = "isActive";
    String IMPORT_ID_PATH_PARAMETER = "importId";

    @RequiresAuthentication
    @GET("audit/events")
    Call<AuditPageDto> auditEvents(
            @Query("action") String action,
            @Query("entity") String entity,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("audit/agent-replays")
    Call<AgentReplayPageDto> agentReplays(
            @Query("channel") String channel,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("catalog-administration/imports")
    Call<CatalogImportDtos.Mutation> uploadCatalogImport(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Header("X-3V-File-Name") String fileName,
            @Header("X-Content-SHA256") String fileSha256,
            @Header("Content-Type") String contentType,
            @Body RequestBody body);

    @RequiresAuthentication
    @GET("catalog-administration/imports/{importId}")
    Call<CatalogImportDtos.View> catalogImport(
            @Path(IMPORT_ID_PATH_PARAMETER) String importId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("catalog-administration/imports/{importId}/confirmation")
    Call<CatalogImportDtos.Mutation> confirmCatalogImport(
            @Path(IMPORT_ID_PATH_PARAMETER) String importId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CatalogImportDtos.Confirmation request);

    @RequiresAuthentication
    @GET("catalog-administration/products")
    Call<CatalogAdministrationDtos.Page> catalogAdministrationProducts(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CATEGORY_ID_QUERY_PARAMETER) Long categoryId,
            @Query(ACTIVE_QUERY_PARAMETER) Boolean active,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("catalog-administration/products/{productId}")
    Call<CatalogAdministrationDtos.Product> catalogAdministrationProduct(
            @Path(PRODUCT_ID_PATH_PARAMETER) long productId);

    @RequiresAuthentication
    @GET("catalog-administration/categories")
    Call<CatalogAdministrationDtos.Categories>
            catalogAdministrationCategories();

    @RequiresAuthentication
    @POST("catalog-administration/products")
    Call<CatalogAdministrationDtos.Mutation>
            createCatalogAdministrationProduct(
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body CatalogAdministrationDtos.CreateRequest request);

    @RequiresAuthentication
    @POST("catalog-administration/products/{productId}/actions")
    Call<CatalogAdministrationDtos.Mutation>
            updateCatalogAdministrationProduct(
                    @Path(PRODUCT_ID_PATH_PARAMETER) long productId,
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body CatalogAdministrationDtos.UpdateRequest request);

    @RequiresAuthentication
    @POST("catalog-administration/products/{productId}/actions")
    Call<CatalogAdministrationDtos.Mutation>
            updateCatalogAdministrationStatus(
                    @Path(PRODUCT_ID_PATH_PARAMETER) long productId,
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body CatalogAdministrationDtos.StatusRequest request);

    @RequiresAuthentication
    @POST("catalog-administration/products/{productId}/actions")
    Call<CatalogAdministrationDtos.Mutation>
            updateCatalogAdministrationKnowledge(
                    @Path(PRODUCT_ID_PATH_PARAMETER) long productId,
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body CatalogAdministrationDtos.KnowledgeRequest request);

    @RequiresAuthentication
    @POST("catalog-administration/categories")
    Call<CatalogAdministrationDtos.CategoryMutation>
            createCatalogAdministrationCategory(
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body CatalogAdministrationDtos.CategoryCreateRequest request);

    @GET("meta")
    Call<MobileApiMeta> meta();

    @RequiresAuthentication
    @GET("painter-administration/options")
    Call<PainterAdministrationDtos.Options> painterAdministrationOptions();

    @RequiresAuthentication
    @GET("painter-administration/painters")
    Call<PainterAdministrationDtos.PainterPage> painterAdministrationPainters(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("painter-administration/painters/{painterId}")
    Call<PainterAdministrationDtos.PainterDetail> painterAdministrationPainter(
            @Path(PAINTER_ID_PATH_PARAMETER) long painterId);

    @RequiresAuthentication
    @GET("painter-administration/access-requests")
    Call<PainterAdministrationDtos.AccessRequestPage>
            painterAdministrationAccessRequests(
                    @Query(CURSOR_QUERY_PARAMETER) String cursor,
                    @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("painter-administration/access-requests/{requestId}")
    Call<PainterAdministrationDtos.AccessRequestDetail>
            painterAdministrationAccessRequest(
                    @Path(ACCESS_REQUEST_ID_PATH_PARAMETER) long requestId);

    @RequiresAuthentication
    @POST("painter-administration/painters")
    Call<PainterAdministrationDtos.Mutation> createPainterAdministrationPainter(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.CreateRequest request);

    @RequiresAuthentication
    @POST("painter-administration/painters/{painterId}/actions")
    Call<PainterAdministrationDtos.Mutation> updatePainterAdministrationStatus(
            @Path(PAINTER_ID_PATH_PARAMETER) long painterId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.StatusRequest request);

    @RequiresAuthentication
    @POST("painter-administration/painters/{painterId}/actions")
    Call<PainterAdministrationDtos.Mutation> updatePainterAdministrationCommission(
            @Path(PAINTER_ID_PATH_PARAMETER) long painterId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.CommissionRequest request);

    @RequiresAuthentication
    @POST("painter-administration/painters/{painterId}/actions")
    Call<PainterAdministrationDtos.Mutation> updatePainterAdministrationManager(
            @Path(PAINTER_ID_PATH_PARAMETER) long painterId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.ManagerRequest request);

    @RequiresAuthentication
    @POST("painter-administration/access-requests/{requestId}/decision")
    Call<PainterAdministrationDtos.Mutation> approvePainterAccessRequest(
            @Path(ACCESS_REQUEST_ID_PATH_PARAMETER) long requestId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.PainterDecisionRequest request);

    @RequiresAuthentication
    @POST("painter-administration/access-requests/{requestId}/decision")
    Call<PainterAdministrationDtos.Mutation> approveManagerAccessRequest(
            @Path(ACCESS_REQUEST_ID_PATH_PARAMETER) long requestId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.ManagerDecisionRequest request);

    @RequiresAuthentication
    @POST("painter-administration/access-requests/{requestId}/decision")
    Call<PainterAdministrationDtos.Mutation> rejectAccessRequest(
            @Path(ACCESS_REQUEST_ID_PATH_PARAMETER) long requestId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PainterAdministrationDtos.RejectDecisionRequest request);

    @RequiresAuthentication
    @GET("user-administration/options")
    Call<UserAdministrationDtos.Options> userAdministrationOptions();

    @RequiresAuthentication
    @GET("user-administration/users")
    Call<UserAdministrationDtos.Page> userAdministrationUsers(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(ROLE_QUERY_PARAMETER) String role,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("user-administration/users/{userId}")
    Call<UserAdministrationDtos.User> userAdministrationUser(
            @Path(USER_ID_PATH_PARAMETER) long userId);

    @RequiresAuthentication
    @POST("user-administration/users/{userId}/actions")
    Call<UserAdministrationDtos.Mutation> assignUserStandardRole(
            @Path(USER_ID_PATH_PARAMETER) long userId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body UserAdministrationDtos.StandardRoleRequest request);

    @RequiresAuthentication
    @POST("user-administration/users/{userId}/actions")
    Call<UserAdministrationDtos.Mutation> assignUserOperationalRole(
            @Path(USER_ID_PATH_PARAMETER) long userId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body UserAdministrationDtos.OperationalRoleRequest request);

    @RequiresAuthentication
    @POST("user-administration/users/{userId}/actions")
    Call<UserAdministrationDtos.Mutation> updateUserAccountStatus(
            @Path(USER_ID_PATH_PARAMETER) long userId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body UserAdministrationDtos.AccountStatusRequest request);

    @RequiresAuthentication
    @GET("organization-administration/organizations")
    Call<OrganizationAdministrationDtos.Page>
            organizationAdministrationOrganizations(
                    @Query(STATUS_QUERY_PARAMETER) String status,
                    @Query(SEARCH_QUERY_PARAMETER) String search,
                    @Query(CURSOR_QUERY_PARAMETER) String cursor,
                    @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("organization-administration/organizations/{organizationId}")
    Call<OrganizationAdministrationDtos.Organization>
            organizationAdministrationOrganization(
                    @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId);

    @RequiresAuthentication
    @POST("organization-administration/organizations")
    Call<OrganizationAdministrationDtos.Mutation>
            createOrganizationAdministrationOrganization(
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body OrganizationAdministrationDtos.CreateRequest request);

    @RequiresAuthentication
    @POST("organization-administration/organizations/{organizationId}/actions")
    Call<OrganizationAdministrationDtos.Mutation>
            renameOrganizationAdministrationOrganization(
                    @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId,
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body OrganizationAdministrationDtos.RenameRequest request);

    @RequiresAuthentication
    @POST("organization-administration/organizations/{organizationId}/actions")
    Call<OrganizationAdministrationDtos.Mutation>
            updateOrganizationAdministrationStatus(
                    @Path(ORGANIZATION_ID_PATH_PARAMETER) long organizationId,
                    @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
                    @Body OrganizationAdministrationDtos.StatusRequest request);

    @POST("auth/challenge")
    Call<AuthChallengeResponse> createChallenge(@Body AuthChallengeRequest request);

    @POST("auth/google")
    Call<GoogleLoginResponse> loginWithGoogle(@Body GoogleLoginRequest request);

    @POST("auth/refresh")
    Call<RefreshResponse> refresh(@Body RefreshRequest request);

    @RequiresAuthentication
    @POST("auth/logout")
    Call<Void> logout();

    @RequiresAuthentication
    @GET("me")
    Call<MeResponse> me();

    @RequiresAuthentication
    @GET("bootstrap")
    Call<BootstrapResponse> bootstrap();

    @RequiresAuthentication
    @GET("dashboard")
    Call<DashboardResponseDto> dashboard(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId);

    @RequiresAuthentication
    @GET("team")
    Call<TeamResponseDto> team(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId);

    @RequiresAuthentication
    @GET("notifications/preferences")
    Call<NotificationPreferencesDto> notificationPreferences();

    @RequiresAuthentication
    @PUT("notifications/preferences")
    Call<NotificationPreferencesDto> updateNotificationPreferences(
            @Body NotificationPreferencesUpdateRequest request);

    @RequiresAuthentication
    @PUT("notifications/registration")
    Call<PushRegistrationResponse> registerPush(
            @Body PushRegistrationRequest request);

    @RequiresAuthentication
    @DELETE("notifications/registration")
    Call<PushRegistrationResponse> unregisterPush();

    @RequiresAuthentication
    @GET("devices")
    Call<AccountDevicePageDto> accountDevices(
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @DELETE("devices/{deviceId}")
    Call<AccountDeviceRevocationDto> revokeAccountDevice(
            @Path(DEVICE_ID_PATH_PARAMETER) String deviceId);

    @RequiresAuthentication
    @GET("sessions")
    Call<AccountSessionPageDto> accountSessions(
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @DELETE("sessions/{sessionId}")
    Call<AccountSessionRevocationDto> revokeAccountSession(
            @Path(SESSION_ID_PATH_PARAMETER) String sessionId);

    @RequiresAuthentication
    @GET("user-administration/users/{userId}/security/devices")
    Call<ManagedAccountDevicePageDto> managedAccountDevices(
            @Path(USER_ID_PATH_PARAMETER) long userId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("user-administration/users/{userId}/security/sessions")
    Call<ManagedAccountSessionPageDto> managedAccountSessions(
            @Path(USER_ID_PATH_PARAMETER) long userId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("user-administration/users/{userId}/security/devices/{deviceId}/revocations/prepare")
    Call<ManagedAccountRevocationDtos.PreparedAction>
            prepareManagedDeviceRevocation(
                    @Path(USER_ID_PATH_PARAMETER) long userId,
                    @Path(DEVICE_ID_PATH_PARAMETER) String deviceId,
                    @Body ManagedAccountRevocationDtos.PrepareRequest request);

    @RequiresAuthentication
    @POST("user-administration/users/{userId}/security/sessions/{sessionId}/revocations/prepare")
    Call<ManagedAccountRevocationDtos.PreparedAction>
            prepareManagedSessionRevocation(
                    @Path(USER_ID_PATH_PARAMETER) long userId,
                    @Path(SESSION_ID_PATH_PARAMETER) String sessionId,
                    @Body ManagedAccountRevocationDtos.PrepareRequest request);

    @RequiresAuthentication
    @POST("user-administration/security/revocations/{actionId}/step-up/challenge")
    Call<ManagedAccountRevocationDtos.ChallengeResponse>
            requestManagedRevocationStepUp(
                    @Path(SECURITY_ADMIN_ACTION_ID_PATH_PARAMETER)
                    String actionId,
                    @Body ManagedAccountRevocationDtos.ChallengeRequest request);

    @RequiresAuthentication
    @POST("user-administration/security/revocations/{actionId}/step-up/google")
    Call<ManagedAccountRevocationDtos.GrantResponse>
            verifyManagedRevocationStepUp(
                    @Path(SECURITY_ADMIN_ACTION_ID_PATH_PARAMETER)
                    String actionId,
                    @Body ManagedAccountRevocationDtos.VerifyRequest request);

    @RequiresAuthentication
    @POST("user-administration/security/revocations/{actionId}/execute")
    Call<ManagedAccountRevocationDtos.Result> executeManagedRevocation(
            @Path(SECURITY_ADMIN_ACTION_ID_PATH_PARAMETER) String actionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body ManagedAccountRevocationDtos.ExecuteRequest request);

    @RequiresAuthentication
    @GET("agent/conversations")
    Call<AgentConversationPageDto> agentConversations(
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit,
            @Query(STATUS_QUERY_PARAMETER) String status);

    @RequiresAuthentication
    @POST("agent/conversations")
    Call<AgentConversationDto> createAgentConversation(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AgentCreateConversationRequest request);

    @RequiresAuthentication
    @GET("agent/conversations/{conversationId}")
    Call<AgentConversationDto> agentConversation(
            @Path(AGENT_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId);

    @RequiresAuthentication
    @GET("agent/conversations/{conversationId}/messages")
    Call<AgentMessagePageDto> agentMessages(
            @Path(AGENT_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("agent/conversations/{conversationId}/turns")
    Call<AgentTurnDto> enqueueAgentTurn(
            @Path(AGENT_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AgentCreateTurnRequest request);

    @RequiresAuthentication
    @GET("agent/turns/{turnId}")
    Call<AgentTurnDto> agentTurn(
            @Path(AGENT_TURN_ID_PATH_PARAMETER) String turnId);

    @RequiresAuthentication
    @POST("agent/turns/{turnId}/cancellation")
    Call<AgentTurnDto> cancelAgentTurn(
            @Path(AGENT_TURN_ID_PATH_PARAMETER) String turnId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AgentCancelTurnRequest request);

    @RequiresAuthentication
    @POST("agent/actions/{actionId}/step-up/challenge")
    Call<AgentStepUpChallengeResponse> requestAgentActionStepUp(
            @Path(AGENT_ACTION_ID_PATH_PARAMETER) String actionId,
            @Body AgentStepUpChallengeRequest request);

    @RequiresAuthentication
    @POST("agent/actions/{actionId}/step-up/google")
    Call<AgentStepUpGrantResponse> verifyAgentActionStepUp(
            @Path(AGENT_ACTION_ID_PATH_PARAMETER) String actionId,
            @Body AgentStepUpVerifyRequest request);

    @RequiresAuthentication
    @POST("agent/actions/{actionId}/decision")
    Call<AgentActionDto> decideAgentAction(
            @Path(AGENT_ACTION_ID_PATH_PARAMETER) String actionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AgentActionDecisionRequest request);

    @RequiresAuthentication
    @GET("attendance/conversations")
    Call<AttendancePageDto> attendanceConversations(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query("channel") String channel,
            @Query("folder") String folder,
            @Query("priority") String priority,
            @Query("assignment") String assignment,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("attendance/conversations/{conversationId}")
    Call<AttendanceConversationDto> attendanceConversation(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId);

    @RequiresAuthentication
    @GET("attendance/conversations/{conversationId}/assignees")
    Call<AttendanceAssigneePageDto> attendanceAssignees(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @PATCH("attendance/conversations/{conversationId}")
    Call<AttendanceManagementResponse> manageAttendanceConversation(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AttendanceManagementRequest request);

    @RequiresAuthentication
    @PUT("attendance/conversations/{conversationId}/read-cursor")
    Call<AttendanceReadCursorResponse> markAttendanceConversationRead(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Body AttendanceMarkReadRequest request);

    @RequiresAuthentication
    @GET("attendance/conversations/{conversationId}/messages")
    Call<AttendanceMessagePageDto> attendanceMessages(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("attendance/conversations/{conversationId}/messages")
    Call<AttendanceReplyResponse> replyToAttendanceConversation(
            @Path(ATTENDANCE_CONVERSATION_ID_PATH_PARAMETER)
            String conversationId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AttendanceReplyRequest request);

    @RequiresAuthentication
    @GET("catalog/priced-products")
    Call<CatalogPageDto> catalogProducts(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId,
            @Query("priceListVersionPublicId") String priceListVersionPublicId,
            @Query("expectedPolicyRevision") int expectedPolicyRevision,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query("categoryId") Long categoryId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("catalog/product-information")
    Call<CatalogPageDto> catalogProductInformation(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query("categoryId") Long categoryId,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("orders")
    Call<OrderPageDto> orders(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(TYPE_QUERY_PARAMETER) String type,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("order-information")
    Call<OrderPageDto> orderInformation(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(TYPE_QUERY_PARAMETER) String type,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("orders/{orderId}")
    Call<OrderDetailDto> order(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId);

    @RequiresAuthentication
    @GET("priced-orders/{orderId}")
    Call<OrderDetailDto> pricedOrder(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId);

    @RequiresAuthentication
    @GET("order-information/{orderId}")
    Call<OrderDetailDto> orderInformation(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId);

    @RequiresAuthentication
    @GET("deliveries")
    Call<DeliveryPageDto> deliveries(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(FROM_QUERY_PARAMETER) String from,
            @Query(TO_QUERY_PARAMETER) String to,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("deliveries/{deliveryId}")
    Call<DeliveryDetailDto> delivery(
            @Path(DELIVERY_ID_PATH_PARAMETER) long deliveryId);

    @RequiresAuthentication
    @GET("delivery-routes")
    Call<DeliveryRoutePageDto> deliveryRoutes(
            @Query("serviceDate") String serviceDate,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId);

    @RequiresAuthentication
    @GET("delivery-routes/{routeKey}")
    Call<DeliveryRoutePlanDto> deliveryRoute(
            @Path(DELIVERY_ROUTE_KEY_PATH_PARAMETER) String routeKey);

    @RequiresAuthentication
    @POST("deliveries/{deliveryId}/start")
    Call<DeliveryMutationResponse> startDelivery(
            @Path(DELIVERY_ID_PATH_PARAMETER) long deliveryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body DeliveryStartRequest request);

    @RequiresAuthentication
    @POST("deliveries/{deliveryId}/completion")
    Call<DeliveryMutationResponse> completeDelivery(
            @Path(DELIVERY_ID_PATH_PARAMETER) long deliveryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body DeliveryCompletionRequest request);

    @RequiresAuthentication
    @GET("delivery-management/organizations")
    Call<DeliveryManagementOrganizationsDto> deliveryManagementOrganizations();

    @RequiresAuthentication
    @GET("delivery-management/drivers")
    Call<DeliveryManagementDriversDto> deliveryManagementDrivers(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId);

    @RequiresAuthentication
    @GET("delivery-management/orders")
    Call<DeliveryManagementPageDto> deliveryManagementOrders(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("delivery-management/orders/{orderId}")
    Call<DeliveryManagementDetailDto> deliveryManagementOrder(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId);

    @RequiresAuthentication
    @POST("delivery-management/orders/{orderId}/schedule")
    Call<DeliveryManagementMutationResponse> scheduleManagedDelivery(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body DeliveryManagementScheduleRequest request);

    @RequiresAuthentication
    @POST("delivery-management/orders/{orderId}/driver-assignment")
    Call<DeliveryManagementMutationResponse> assignManagedDeliveryDriver(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body DeliveryManagementAssignmentRequest request);

    @RequiresAuthentication
    @POST("delivery-management/orders/{orderId}/completion")
    Call<DeliveryManagementMutationResponse> completeManagedDelivery(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body DeliveryManagementCompletionRequest request);

    @RequiresAuthentication
    @GET("commissions")
    Call<CommissionPageDto> commissions(
            @Query(SCOPE_QUERY_PARAMETER) String scope,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query("kind") String kind,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(FROM_QUERY_PARAMETER) String from,
            @Query(TO_QUERY_PARAMETER) String to,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("commissions/{commissionId}")
    Call<CommissionDetailDto> commission(
            @Path(COMMISSION_ID_PATH_PARAMETER) long commissionId,
            @Query(SCOPE_QUERY_PARAMETER) String scope);

    @RequiresAuthentication
    @POST("commissions/{commissionId}/approve")
    Call<CommissionMutationResponse> approveCommission(
            @Path(COMMISSION_ID_PATH_PARAMETER) long commissionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CommissionApprovalRequest request);

    @RequiresAuthentication
    @POST("commissions/{commissionId}/cancel")
    Call<CommissionMutationResponse> cancelCommission(
            @Path(COMMISSION_ID_PATH_PARAMETER) long commissionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CommissionCancellationRequest request);

    @RequiresAuthentication
    @POST("commissions/{commissionId}/payment")
    Call<CommissionMutationResponse> payCommission(
            @Path(COMMISSION_ID_PATH_PARAMETER) long commissionId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CommissionPaymentRequest request);

    @RequiresAuthentication
    @GET("personal-finance/entries")
    Call<PersonalFinancePageDto> personalFinanceEntries(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(TYPE_QUERY_PARAMETER) String type,
            @Query("due") String due,
            @Query("dateBasis") String dateBasis,
            @Query("dueFrom") String dueFrom,
            @Query("dueTo") String dueTo,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("personal-finance/entries/{entryId}")
    Call<PersonalFinanceDetailDto> personalFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId);

    @RequiresAuthentication
    @POST("personal-finance/entries")
    Call<PersonalFinanceMutationResponse> createPersonalFinanceEntry(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PersonalFinanceCreateRequest request);

    @RequiresAuthentication
    @POST("personal-finance/entries/{entryId}/settlement")
    Call<PersonalFinanceMutationResponse> settlePersonalFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PersonalFinanceSettlementRequest request);

    @RequiresAuthentication
    @POST("personal-finance/entries/{entryId}/cancellation")
    Call<PersonalFinanceMutationResponse> cancelPersonalFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body PersonalFinanceCancellationRequest request);

    @RequiresAuthentication
    @GET("corporate-finance/organizations")
    Call<CorporateFinanceOrganizationPageDto> corporateFinanceOrganizations(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("corporate-finance/entries")
    Call<CorporateFinancePageDto> corporateFinanceEntries(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(TYPE_QUERY_PARAMETER) String type,
            @Query("source") String source,
            @Query("due") String due,
            @Query("dateBasis") String dateBasis,
            @Query("dueFrom") String dueFrom,
            @Query("dueTo") String dueTo,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("corporate-finance/entries/{entryId}")
    Call<CorporateFinanceDetailDto> corporateFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId);

    @RequiresAuthentication
    @POST("corporate-finance/entries")
    Call<CorporateFinanceMutationResponse> createCorporateFinanceEntry(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CorporateFinanceCreateRequest request);

    @RequiresAuthentication
    @POST("corporate-finance/entries/{entryId}/settlement")
    Call<CorporateFinanceMutationResponse> settleCorporateFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CorporateFinanceSettlementRequest request);

    @RequiresAuthentication
    @POST("corporate-finance/entries/{entryId}/cancellation")
    Call<CorporateFinanceMutationResponse> cancelCorporateFinanceEntry(
            @Path(FINANCE_ENTRY_ID_PATH_PARAMETER) long entryId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CorporateFinanceCancellationRequest request);

    @RequiresAuthentication
    @GET("appointments")
    Call<AppointmentPageDto> appointments(
            @Query(SCOPE_QUERY_PARAMETER) String scope,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query("kind") String kind,
            @Query(FROM_QUERY_PARAMETER) String from,
            @Query(TO_QUERY_PARAMETER) String to,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("appointments/{appointmentId}")
    Call<AppointmentDetailDto> appointment(
            @Path(APPOINTMENT_ID_PATH_PARAMETER) long appointmentId,
            @Query(SCOPE_QUERY_PARAMETER) String scope);

    @RequiresAuthentication
    @GET("appointments/responsibles")
    Call<AppointmentResponsiblePageDto> appointmentResponsibles(
            @Query(SCOPE_QUERY_PARAMETER) String scope,
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) Long organizationId,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @POST("appointments")
    Call<AppointmentMutationResponse> createAppointment(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AppointmentCreateRequest request);

    @RequiresAuthentication
    @PATCH("appointments/{appointmentId}")
    Call<AppointmentMutationResponse> updateAppointment(
            @Path(APPOINTMENT_ID_PATH_PARAMETER) long appointmentId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AppointmentUpdateRequest request);

    @RequiresAuthentication
    @POST("appointments/{appointmentId}/status")
    Call<AppointmentMutationResponse> transitionAppointment(
            @Path(APPOINTMENT_ID_PATH_PARAMETER) long appointmentId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body AppointmentTransitionRequest request);

    @RequiresAuthentication
    @POST("orders/{orderId}/status")
    Call<OrderMutationResponse> transitionOrderStatus(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body OrderStatusMutationRequest request);

    @RequiresAuthentication
    @POST("orders/{orderId}/cancellation")
    Call<OrderMutationResponse> cancelOrder(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body OrderCancellationRequest request);

    @RequiresAuthentication
    @POST("orders/{orderId}/payment-receipt")
    Call<OrderMutationResponse> recordOrderPayment(
            @Path(ORDER_ID_PATH_PARAMETER) long orderId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body OrderPaymentReceiptRequest request);

    @RequiresAuthentication
    @POST("material-quotes/{quoteId}/order")
    Call<OrderConversionResponse> convertMaterialQuoteToOrder(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body OrderConversionRequest request);

    @RequiresAuthentication
    @GET("customers")
    Call<CustomerPageDto> customers(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("customers/{customerId}")
    Call<CustomerDetailDto> customer(
            @Path("customerId") long customerId);

    @RequiresAuthentication
    @POST("customers")
    Call<CustomerMutationResponse> createCustomer(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CustomerCreateRequest request);

    @RequiresAuthentication
    @PATCH("customers/{customerId}")
    Call<CustomerMutationResponse> updateCustomer(
            @Path("customerId") long customerId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body CustomerUpdateRequest request);

    @RequiresAuthentication
    @GET("pricing/context")
    Call<PricingContextDto> pricingContext(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId);

    @RequiresAuthentication
    @GET("pricing/tint-configurations")
    Call<TintSalesDtos.ConfigurationResponse> tintSalesConfigurations(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId,
            @Query("priceListVersionPublicId") String priceListVersionPublicId,
            @Query("expectedPolicyRevision") int expectedPolicyRevision);

    @RequiresAuthentication
    @GET("pricing/tint-colors")
    Call<TintSalesDtos.ColorResponse> tintSalesColors(
            @Query(ORGANIZATION_ID_QUERY_PARAMETER) long organizationId,
            @Query("priceListVersionPublicId") String priceListVersionPublicId,
            @Query("expectedPolicyRevision") int expectedPolicyRevision,
            @Query("sourceSystem") String sourceSystem,
            @Query("lineName") String lineName,
            @Query("finishName") String finishName,
            @Query("packageName") String packageName,
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("material-quotes")
    Call<MaterialQuotePageDto> materialQuotes(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("material-quotes/{quoteId}")
    Call<MaterialQuoteDetailDto> materialQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId);

    @Streaming
    @RequiresAuthentication
    @GET("material-quotes/{quoteId}/pdf")
    Call<ResponseBody> materialQuotePdf(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId);

    @RequiresAuthentication
    @POST("material-quotes/preview")
    Call<MaterialQuoteCreatePreviewResponse> previewMaterialQuoteCreate(
            @Body MaterialQuoteCreatePreviewRequest request);

    @RequiresAuthentication
    @POST("material-quotes")
    Call<MaterialQuoteMutationResponse> createMaterialQuote(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body MaterialQuoteCreateRequest request);

    @RequiresAuthentication
    @POST("material-quotes/{quoteId}/duplicate")
    Call<MaterialQuoteMutationResponse> duplicateMaterialQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey);

    @RequiresAuthentication
    @POST("material-quotes/{quoteId}/preview")
    Call<MaterialQuoteUpdatePreviewResponse> previewMaterialQuoteUpdate(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Body MaterialQuoteUpdatePreviewRequest request);

    @RequiresAuthentication
    @PATCH("material-quotes/{quoteId}")
    Call<MaterialQuoteMutationResponse> updateMaterialQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body MaterialQuoteUpdateRequest request);

    @RequiresAuthentication
    @POST("material-quotes/{quoteId}/status")
    Call<MaterialQuoteStatusMutationResponse> transitionMaterialQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body MaterialQuoteStatusMutationRequest request);

    @RequiresAuthentication
    @GET("labor-quotes")
    Call<LaborQuotePageDto> laborQuotes(
            @Query(SEARCH_QUERY_PARAMETER) String search,
            @Query(STATUS_QUERY_PARAMETER) String status,
            @Query(VIEW_QUERY_PARAMETER) String view,
            @Query(CURSOR_QUERY_PARAMETER) String cursor,
            @Query(LIMIT_QUERY_PARAMETER) int limit);

    @RequiresAuthentication
    @GET("labor-quotes/{quoteId}")
    Call<LaborQuoteDetailDto> laborQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId);

    @Streaming
    @RequiresAuthentication
    @GET("labor-quotes/{quoteId}/pdf")
    Call<ResponseBody> laborQuotePdf(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId);

    @RequiresAuthentication
    @POST("labor-quotes")
    Call<LaborQuoteMutationResponse> createLaborQuote(
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body LaborQuoteCreateRequest request);

    @RequiresAuthentication
    @POST("labor-quotes/{quoteId}/duplicate")
    Call<LaborQuoteMutationResponse> duplicateLaborQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey);

    @RequiresAuthentication
    @PATCH("labor-quotes/{quoteId}")
    Call<LaborQuoteMutationResponse> updateLaborQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body LaborQuoteUpdateRequest request);

    @RequiresAuthentication
    @POST("labor-quotes/{quoteId}/status")
    Call<LaborQuoteStatusMutationResponse> transitionLaborQuote(
            @Path(QUOTE_ID_PATH_PARAMETER) long quoteId,
            @Header(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Body LaborQuoteStatusMutationRequest request);
}
