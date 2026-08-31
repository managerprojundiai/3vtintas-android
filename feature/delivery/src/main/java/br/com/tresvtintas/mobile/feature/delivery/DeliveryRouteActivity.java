package br.com.tresvtintas.mobile.feature.delivery;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePlan;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteState;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryRouteActivityBinding;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

public final class DeliveryRouteActivity extends AppCompatActivity {
    private static final String STATE_SERVICE_DATE = "delivery.route.serviceDate";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final DeliveryRouteController.Listener listener = this::render;
    private DeliveryRouteActivityBinding binding;
    private DeliveryRouteRenderer renderer;
    private Optional<DeliveryRouteController> controller = Optional.empty();
    private Optional<DeliveryRoutePlan> currentRoute = Optional.empty();
    private LocalDate selectedDate;

    public static Intent intent(Context context) {
        return new Intent(context, DeliveryRouteActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeliveryPrivacy.protect(this);
        EdgeToEdge.enable(this);
        selectedDate = savedDate(state);
        binding = DeliveryRouteActivityBinding.inflate(getLayoutInflater());
        DeliveryRouteStopAdapter adapter = new DeliveryRouteStopAdapter();
        renderer = new DeliveryRouteRenderer(binding, adapter);
        setContentView(binding.getRoot());
        DeliveryInsets.applySystemBars(binding.getRoot());
        binding.deliveryRouteStops.setLayoutManager(new LinearLayoutManager(this));
        binding.deliveryRouteStops.setAdapter(adapter);
        binding.deliveryRouteToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.deliveryRoutePrevious.setOnClickListener(
                ignored -> selectDate(selectedDate.minusDays(1)));
        binding.deliveryRouteNext.setOnClickListener(
                ignored -> selectDate(selectedDate.plusDays(1)));
        binding.deliveryRouteDate.setOnClickListener(ignored -> showDatePicker());
        binding.deliveryRouteRefresh.setOnClickListener(
                ignored -> controller.ifPresent(DeliveryRouteController::refresh));
        binding.deliveryRouteRetry.setOnClickListener(
                ignored -> controller.ifPresent(value -> value.open(selectedDate)));
        binding.deliveryRouteNavigate.setOnClickListener(ignored ->
                currentRoute.flatMap(DeliveryRoutePlan::nextStop)
                        .ifPresent(stop -> DeliveryRouteNavigator.open(this, stop)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DeliveryFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty() || runtime.orElseThrow().routeRepository().isEmpty()) {
            render(DeliveryRouteState.error(selectedDate, new DeliveryException(
                    DeliveryFailureKind.ACCESS_REVOKED,
                    "Delivery route runtime is unavailable.")));
            return;
        }
        DeliveryFeatureRuntime value = runtime.orElseThrow();
        DeliveryRouteController next = new DeliveryRouteController(
                value.routeRepository().orElseThrow(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this),
                selectedDate);
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(selectedDate);
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        currentRoute = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_SERVICE_DATE, selectedDate.toString());
        super.onSaveInstanceState(state);
    }

    private void selectDate(LocalDate value) {
        selectedDate = value;
        currentRoute = Optional.empty();
        controller.ifPresent(item -> item.open(value));
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> selectDate(
                        LocalDate.of(year, month + 1, day)),
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()).show();
    }

    private void render(DeliveryRouteState state) {
        selectedDate = state.serviceDate();
        currentRoute = state.page().flatMap(page -> page.current());
        renderer.render(state);
    }

    private Optional<DeliveryFeatureRuntime> runtime() {
        return getApplication() instanceof DeliveryRuntimeProvider provider
                ? provider.deliveryRuntime()
                : Optional.empty();
    }

    private static LocalDate savedDate(Bundle state) {
        if (state != null) {
            String value = state.getString(STATE_SERVICE_DATE);
            if (value != null) {
                try {
                    return LocalDate.parse(value);
                } catch (java.time.format.DateTimeParseException ignored) {
                    // A malformed process-restoration value falls back to today.
                }
            }
        }
        return LocalDate.now(BUSINESS_ZONE);
    }
}
