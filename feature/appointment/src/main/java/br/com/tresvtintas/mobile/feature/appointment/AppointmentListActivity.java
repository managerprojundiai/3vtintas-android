package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.appointment.AgendaController;
import br.com.tresvtintas.mobile.core.appointment.AgendaEntry;
import br.com.tresvtintas.mobile.core.appointment.AgendaEntrySource;
import br.com.tresvtintas.mobile.core.appointment.AgendaState;
import br.com.tresvtintas.mobile.core.appointment.AgendaStateListener;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityListBinding;
import br.com.tresvtintas.mobile.feature.finance.FinanceDetailActivity;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryDetailActivity;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

public final class AppointmentListActivity extends AppCompatActivity {
    private static final int CALENDAR_COLUMNS = 7;
    private static final String STATE_MONTH =
            "br.com.tresvtintas.mobile.agenda.MONTH";
    private static final String STATE_DATE =
            "br.com.tresvtintas.mobile.agenda.DATE";
    private final ZoneId zoneId = ZoneId.systemDefault();
    private final AgendaStateListener listener = this::render;
    private AppointmentActivityListBinding binding;
    private AgendaRenderer renderer;
    private Optional<AgendaController> controller = Optional.empty();
    private Optional<AppointmentFeatureRuntime> runtime = Optional.empty();
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    public static Intent intent(Context context) {
        return new Intent(context, AppointmentListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppointmentPrivacy.protect(this);
        EdgeToEdge.enable(this);
        restore(state);
        binding = AppointmentActivityListBinding.inflate(
                getLayoutInflater());
        AgendaCalendarAdapter calendarAdapter =
                new AgendaCalendarAdapter(this::selectDate);
        AgendaEntryAdapter entryAdapter =
                new AgendaEntryAdapter(zoneId, this::openEntry);
        renderer = new AgendaRenderer(
                binding,
                calendarAdapter,
                entryAdapter,
                runtime().map(value -> value.financeRepository().isPresent())
                        .orElse(false));
        setContentView(binding.getRoot());
        AppointmentInsets.applySystemBars(binding.getRoot());
        binding.agendaCalendar.setLayoutManager(
                new GridLayoutManager(this, CALENDAR_COLUMNS));
        binding.agendaCalendar.setHasFixedSize(true);
        binding.agendaCalendar.setItemAnimator(null);
        binding.agendaCalendar.setAdapter(calendarAdapter);
        binding.appointmentList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.appointmentList.setAdapter(entryAdapter);
        binding.appointmentToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.agendaPreviousMonth.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgendaController::previousMonth));
        binding.agendaNextMonth.setOnClickListener(
                ignored -> controller.ifPresent(
                        AgendaController::nextMonth));
        binding.agendaToday.setOnClickListener(
                ignored -> controller.ifPresent(value ->
                        value.today(LocalDate.now(zoneId))));
        binding.appointmentRefresh.setOnClickListener(
                ignored -> controller.ifPresent(AgendaController::refresh));
        binding.appointmentRetry.setOnClickListener(
                ignored -> controller.ifPresent(AgendaController::open));
        binding.appointmentCreate.setOnClickListener(
                ignored -> startActivity(
                        AppointmentFormActivity.createIntent(this)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        if (runtime.isEmpty()) {
            binding.appointmentCreate.setVisibility(View.GONE);
            return;
        }
        AppointmentFeatureRuntime value = runtime.orElseThrow();
        binding.appointmentCreate.setVisibility(
                value.canWrite() ? View.VISIBLE : View.GONE);
        AgendaController next = new AgendaController(
                value.repository(),
                value.financeRepository(),
                value.deliveryRepository(),
                value.scope(),
                value.organizationId(),
                zoneId,
                displayedMonth,
                selectedDate,
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        runtime = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_MONTH, displayedMonth.toString());
        state.putString(STATE_DATE, selectedDate.toString());
        super.onSaveInstanceState(state);
    }

    private void selectDate(LocalDate date) {
        controller.ifPresent(value -> value.select(date));
    }

    private void openEntry(AgendaEntry entry) {
        if (entry.source() == AgendaEntrySource.FINANCE) {
            runtime.flatMap(AppointmentFeatureRuntime::financeRoute)
                    .ifPresent(route -> startActivity(
                            FinanceDetailActivity.intent(
                                    this,
                                    entry.sourceId(),
                                    route)));
            return;
        }
        if (entry.source() == AgendaEntrySource.DELIVERY) {
            startActivity(DeliveryDetailActivity.intent(
                    this,
                    entry.sourceId()));
            return;
        }
        startActivity(AppointmentDetailActivity.intent(
                this,
                entry.sourceId()));
    }

    private void render(AgendaState state) {
        state.snapshot().ifPresent(snapshot -> {
            displayedMonth = snapshot.month();
            selectedDate = snapshot.selectedDate();
        });
        renderer.render(state);
    }

    private void restore(Bundle state) {
        if (state == null) {
            return;
        }
        String monthValue = state.getString(STATE_MONTH);
        String dateValue = state.getString(STATE_DATE);
        if (monthValue == null || dateValue == null) {
            return;
        }
        try {
            displayedMonth = YearMonth.parse(monthValue);
            selectedDate = LocalDate.parse(dateValue);
        } catch (java.time.format.DateTimeParseException ignored) {
            displayedMonth = YearMonth.now();
            selectedDate = LocalDate.now();
        }
    }

    private Optional<AppointmentFeatureRuntime> runtime() {
        return getApplication() instanceof AppointmentRuntimeProvider provider
                ? provider.appointmentRuntime()
                : Optional.empty();
    }
}
