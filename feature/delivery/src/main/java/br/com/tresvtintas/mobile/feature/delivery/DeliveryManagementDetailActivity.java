package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementActionController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementActionState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementCommand;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetailController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetailState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDriver;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryManagementActivityDetailBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class DeliveryManagementDetailActivity
        extends AppCompatActivity {
    private static final String EXTRA_ORGANIZATION_ID =
            "br.com.tresvtintas.mobile.delivery.MANAGEMENT_ORGANIZATION_ID";
    private static final String EXTRA_ORDER_ID =
            "br.com.tresvtintas.mobile.delivery.MANAGEMENT_ORDER_ID";
    private static final String STATE_ACTION_KEY =
            "delivery_management_action_key";
    private static final String STATE_ACTION_FINGERPRINT =
            "delivery_management_action_fingerprint";
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DeliveryManagementDetailController.Listener listener =
            this::render;
    private final DeliveryManagementActionController.Listener actionListener =
            this::renderAction;
    private DeliveryManagementActivityDetailBinding binding;
    private DeliveryManagementDetailRenderer renderer;
    private Optional<DeliveryManagementDetailController> controller =
            Optional.empty();
    private Optional<DeliveryManagementActionController> actionController =
            Optional.empty();
    private Optional<DeliveryManagementDetail> currentDetail =
            Optional.empty();
    private List<DeliveryManagementDriver> drivers = List.of();
    private DeliveryManagementActionAttempt actionAttempt =
            new DeliveryManagementActionAttempt();
    private long organizationId;
    private long orderId;
    private boolean canSchedule;
    private boolean canAssign;
    private boolean canComplete;

    public static Intent intent(
            Context context,
            long organizationId,
            long orderId) {
        return new Intent(context, DeliveryManagementDetailActivity.class)
                .putExtra(EXTRA_ORGANIZATION_ID, organizationId)
                .putExtra(EXTRA_ORDER_ID, orderId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeliveryPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = DeliveryManagementActivityDetailBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());
        DeliveryInsets.applySystemBars(binding.getRoot());
        organizationId = getIntent().getLongExtra(
                EXTRA_ORGANIZATION_ID,
                0);
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, 0);
        binding.deliveryManagementDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.deliveryManagementDetailRetry.setOnClickListener(
                ignored -> load());
        binding.deliveryManagementActionSchedule.setOnClickListener(
                ignored -> pickSchedule());
        binding.deliveryManagementActionAssign.setOnClickListener(
                ignored -> selectDriver());
        binding.deliveryManagementActionUnassign.setOnClickListener(
                ignored -> confirmUnassignment());
        binding.deliveryManagementActionComplete.setOnClickListener(
                ignored -> confirmCompletion());
        if (state != null) {
            actionAttempt = DeliveryManagementActionAttempt.restored(
                    state.getString(STATE_ACTION_KEY, ""),
                    state.getString(STATE_ACTION_FINGERPRINT, ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_ACTION_KEY, actionAttempt.key());
        state.putString(
                STATE_ACTION_FINGERPRINT,
                actionAttempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DeliveryFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()
                || !runtime.orElseThrow().canManage()
                || runtime.orElseThrow().managementRepository().isEmpty()) {
            renderer = new DeliveryManagementDetailRenderer(
                    binding,
                    false,
                    false,
                    false);
            render(DeliveryManagementDetailState.error(
                    new DeliveryException(
                            DeliveryFailureKind.ACCESS_REVOKED,
                            "Delivery management runtime is unavailable.")));
            return;
        }
        DeliveryFeatureRuntime value = runtime.orElseThrow();
        canSchedule = value.canSchedule();
        canAssign = value.canAssign();
        canComplete = value.canCompleteManagement();
        renderer = new DeliveryManagementDetailRenderer(
                binding,
                canSchedule,
                canAssign,
                canComplete);
        DeliveryManagementDetailController next =
                new DeliveryManagementDetailController(
                        value.managementRepository().orElseThrow(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        DeliveryManagementActionController nextAction =
                new DeliveryManagementActionController(
                        value.managementRepository().orElseThrow(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        actionController = Optional.of(nextAction);
        next.subscribe(listener);
        nextAction.subscribe(actionListener);
        load();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        actionController.ifPresent(value -> {
            value.unsubscribe(actionListener);
            value.close();
        });
        actionController = Optional.empty();
        currentDetail = Optional.empty();
        drivers = List.of();
        super.onStop();
    }

    private void load() {
        controller.ifPresent(value -> value.load(organizationId, orderId));
    }

    private void render(DeliveryManagementDetailState state) {
        if (renderer == null) {
            return;
        }
        if (state.phase() == DeliveryManagementDetailState.Phase.READY) {
            currentDetail = state.detail();
            drivers = state.drivers();
        } else if (state.phase() == DeliveryManagementDetailState.Phase.ERROR) {
            currentDetail = Optional.empty();
            drivers = List.of();
        }
        renderer.render(state);
    }

    private void pickSchedule() {
        if (!authorized(DeliveryManagementAction.SCHEDULE, canSchedule)) {
            return;
        }
        ZonedDateTime initial = currentDetail
                .flatMap(value -> value.summary().delivery())
                .flatMap(value -> value.scheduledAt())
                .orElseGet(() -> Instant.now().plusSeconds(3_600))
                .atZone(BUSINESS_ZONE);
        long selection = initial.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText(R.string.delivery_management_pick_date)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(value -> {
            LocalDate date = Instant.ofEpochMilli(value)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            pickTime(date, initial.getHour(), initial.getMinute());
        });
        picker.show(
                getSupportFragmentManager(),
                "delivery-management-date");
    }

    private void pickTime(LocalDate date, int hour, int minute) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText(R.string.delivery_management_pick_time)
                .setHour(hour)
                .setMinute(minute)
                .build();
        picker.addOnPositiveButtonClickListener(ignored -> {
            Instant scheduledAt = date.atTime(
                            picker.getHour(),
                            picker.getMinute())
                    .atZone(BUSINESS_ZONE)
                    .toInstant();
            confirmSchedule(scheduledAt);
        });
        picker.show(
                getSupportFragmentManager(),
                "delivery-management-time");
    }

    private void confirmSchedule(Instant scheduledAt) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delivery_management_confirm_schedule_title)
                .setMessage(getString(
                        R.string.delivery_management_confirm_schedule_message,
                        DISPLAY.format(scheduledAt.atZone(BUSINESS_ZONE))))
                .setNegativeButton(R.string.delivery_cancel, null)
                .setPositiveButton(
                        R.string.delivery_confirm,
                        (dialog, ignored) -> execute(
                                new DeliveryManagementCommand.Schedule(
                                        organizationId,
                                        orderId,
                                        revision(),
                                        scheduledAt,
                                        DEFAULT_DURATION_MINUTES),
                                scheduledAt.toString()))
                .show();
    }

    private void selectDriver() {
        if (!authorized(DeliveryManagementAction.ASSIGN, canAssign)) {
            return;
        }
        if (drivers.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.delivery_management_no_drivers)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String[] names = drivers.stream()
                .map(DeliveryManagementDriver::displayName)
                .toArray(String[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delivery_management_action_assign)
                .setItems(names, (dialog, which) ->
                        confirmAssignment(drivers.get(which)))
                .setNegativeButton(R.string.delivery_cancel, null)
                .show();
    }

    private void confirmAssignment(DeliveryManagementDriver driver) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(
                        R.string.delivery_management_confirm_assignment_title,
                        driver.displayName()))
                .setMessage(
                        R.string.delivery_management_confirm_assignment_message)
                .setNegativeButton(R.string.delivery_cancel, null)
                .setPositiveButton(
                        R.string.delivery_confirm,
                        (dialog, ignored) -> execute(
                                new DeliveryManagementCommand.Assignment(
                                        organizationId,
                                        orderId,
                                        revision(),
                                        OptionalLong.of(driver.userId())),
                                Long.toString(driver.userId())))
                .show();
    }

    private void confirmUnassignment() {
        if (!authorized(DeliveryManagementAction.UNASSIGN, canAssign)) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delivery_management_confirm_unassignment_title)
                .setMessage(
                        R.string.delivery_management_confirm_unassignment_message)
                .setNegativeButton(R.string.delivery_cancel, null)
                .setPositiveButton(
                        R.string.delivery_confirm,
                        (dialog, ignored) -> execute(
                                new DeliveryManagementCommand.Assignment(
                                        organizationId,
                                        orderId,
                                        revision(),
                                        OptionalLong.empty()),
                                "none"))
                .show();
    }

    private void confirmCompletion() {
        if (!authorized(DeliveryManagementAction.COMPLETE, canComplete)) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delivery_management_confirm_completion_title)
                .setMessage(
                        R.string.delivery_management_confirm_completion_message)
                .setNegativeButton(R.string.delivery_cancel, null)
                .setPositiveButton(
                        R.string.delivery_confirm,
                        (dialog, ignored) -> execute(
                                new DeliveryManagementCommand.Completion(
                                        organizationId,
                                        orderId,
                                        revision()),
                                "complete"))
                .show();
    }

    private void execute(
            DeliveryManagementCommand command,
            String qualifier) {
        String key = actionAttempt.keyFor(
                command.action(),
                organizationId,
                orderId,
                command.expectedOrderRevision(),
                qualifier);
        actionController.ifPresent(value -> value.execute(command, key));
    }

    private int revision() {
        return currentDetail.orElseThrow().summary().order().revision();
    }

    private void renderAction(DeliveryManagementActionState state) {
        if (renderer == null) {
            return;
        }
        if (state.phase() == DeliveryManagementActionState.Phase.RUNNING) {
            renderer.setActionsBusy(true);
            return;
        }
        renderer.setActionsBusy(false);
        if (state.phase() == DeliveryManagementActionState.Phase.SUCCESS) {
            boolean replayed = state.result().orElseThrow().replayed();
            actionAttempt.reset();
            renderer.showActionNotice(
                    replayed
                            ? R.string.delivery_action_replayed
                            : R.string.delivery_action_success,
                    null);
            load();
        } else if (state.phase() == DeliveryManagementActionState.Phase.ERROR) {
            DeliveryFailureKind failure = state.failure().orElseThrow();
            if (failure == DeliveryFailureKind.CONFLICT
                    || failure
                            == DeliveryFailureKind.IDEMPOTENCY_KEY_REUSED) {
                actionAttempt.reset();
            }
            renderer.showActionNotice(
                    failure == DeliveryFailureKind.CONFLICT
                            ? R.string.delivery_action_conflict
                            : DeliveryText.failure(failure),
                    state.requestId().orElse(null));
            if (failure == DeliveryFailureKind.CONFLICT) {
                load();
            }
        }
    }

    private boolean authorized(
            DeliveryManagementAction action,
            boolean capability) {
        return capability && currentDetail
                .map(value -> value.summary()
                        .allowedActions()
                        .contains(action))
                .orElse(false);
    }

    private Optional<DeliveryFeatureRuntime> runtime() {
        return getApplication()
                        instanceof DeliveryRuntimeProvider provider
                ? provider.deliveryRuntime()
                : Optional.empty();
    }
}
