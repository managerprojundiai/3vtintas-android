package br.com.tresvtintas.mobile.feature.appointment;

import android.view.View;
import br.com.tresvtintas.mobile.core.appointment.AgendaDay;
import br.com.tresvtintas.mobile.core.appointment.AgendaMonthSnapshot;
import br.com.tresvtintas.mobile.core.appointment.AgendaState;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityListBinding;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AgendaRenderer {
    private final AppointmentActivityListBinding binding;
    private final AgendaCalendarAdapter calendarAdapter;
    private final AgendaEntryAdapter entryAdapter;
    private final boolean financeAvailable;
    private final NumberFormat money =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final DateTimeFormatter monthFormatter =
            DateTimeFormatter.ofPattern(
                    "MMMM 'de' yyyy",
                    new Locale("pt", "BR"));
    private final DateTimeFormatter dayFormatter =
            DateTimeFormatter.ofPattern(
                    "EEEE, d 'de' MMMM",
                    new Locale("pt", "BR"));

    AgendaRenderer(
            AppointmentActivityListBinding binding,
            AgendaCalendarAdapter calendarAdapter,
            AgendaEntryAdapter entryAdapter,
            boolean financeAvailable) {
        this.binding = binding;
        this.calendarAdapter = calendarAdapter;
        this.entryAdapter = entryAdapter;
        this.financeAvailable = financeAvailable;
    }

    void render(AgendaState state) {
        boolean busy = state.phase() == AgendaState.Phase.LOADING
                || state.phase() == AgendaState.Phase.REFRESHING;
        binding.appointmentProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.appointmentRefresh.setEnabled(!busy);
        binding.agendaPreviousMonth.setEnabled(true);
        binding.agendaNextMonth.setEnabled(true);
        binding.agendaToday.setEnabled(true);
        binding.appointmentNoticeCard.setVisibility(View.GONE);
        binding.appointmentRetry.setVisibility(View.GONE);
        if (state.phase() == AgendaState.Phase.ERROR) {
            error(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                snapshot -> snapshot(state, snapshot),
                this::withoutSnapshot);
    }

    private void error(AgendaState state) {
        calendarAdapter.submit(
                java.time.YearMonth.now(),
                java.time.LocalDate.now(),
                emptyDays());
        entryAdapter.submit(java.util.List.of());
        binding.agendaDaySummary.setVisibility(View.GONE);
        binding.appointmentList.setVisibility(View.GONE);
        binding.appointmentEmptyGroup.setVisibility(View.VISIBLE);
        binding.appointmentEmptyTitle.setText(R.string.agenda_title);
        binding.appointmentEmptyMessage.setText(
                AgendaText.failure(state.failure().orElseThrow()));
        binding.appointmentRetry.setVisibility(View.VISIBLE);
        support(state);
    }

    private void snapshot(
            AgendaState state,
            AgendaMonthSnapshot snapshot) {
        calendarAdapter.submit(
                snapshot.month(),
                snapshot.selectedDate(),
                snapshot.visibleDays());
        AgendaDay day = snapshot.selectedDay();
        entryAdapter.submit(day.entries());
        binding.agendaMonthTitle.setText(
                titleCase(monthFormatter.format(snapshot.month())));
        binding.agendaSelectedDate.setText(
                titleCase(dayFormatter.format(day.date())));
        binding.agendaDayTotals.setText(binding.getRoot()
                .getContext()
                .getString(
                        R.string.agenda_day_totals,
                        money.format(day.receivableTotal()),
                        money.format(day.payableTotal()),
                        money.format(day.expenseTotal())));
        if (state.phase() == AgendaState.Phase.LOADING) {
            binding.agendaDaySummary.setVisibility(View.VISIBLE);
            binding.agendaDayTotals.setVisibility(View.GONE);
            binding.appointmentList.setVisibility(View.GONE);
            binding.appointmentEmptyGroup.setVisibility(View.GONE);
            return;
        }
        binding.agendaDayTotals.setVisibility(View.VISIBLE);
        boolean empty = day.entries().isEmpty();
        binding.agendaDaySummary.setVisibility(View.VISIBLE);
        binding.appointmentList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.appointmentEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.appointmentEmptyTitle.setText(R.string.agenda_empty_title);
        binding.appointmentEmptyMessage.setText(R.string.agenda_empty_message);
        if (state.failure().isPresent()) {
            notice(
                    R.string.agenda_warning_stale,
                    state.requestId());
        } else if (!financeAvailable) {
            notice(
                    R.string.agenda_finance_unavailable,
                    java.util.Optional.empty());
        }
    }

    private void withoutSnapshot() {
        binding.agendaDaySummary.setVisibility(View.GONE);
        binding.appointmentList.setVisibility(View.GONE);
        binding.appointmentEmptyGroup.setVisibility(View.GONE);
    }

    private void notice(
            int message,
            java.util.Optional<String> requestId) {
        String text = binding.getRoot().getContext().getString(message);
        if (requestId.isPresent()) {
            text = text
                    + "\n"
                    + binding.getRoot().getContext().getString(
                            R.string.appointment_support_code,
                            requestId.orElseThrow());
        }
        binding.appointmentNotice.setText(text);
        binding.appointmentNoticeCard.setVisibility(View.VISIBLE);
    }

    private void support(AgendaState state) {
        state.requestId().ifPresent(value -> {
            binding.appointmentNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.appointment_support_code,
                            value));
            binding.appointmentNoticeCard.setVisibility(View.VISIBLE);
        });
    }

    private static java.util.List<AgendaDay> emptyDays() {
        AgendaMonthSnapshot.DateWindow window = AgendaMonthSnapshot.window(
                java.time.YearMonth.now(),
                java.time.ZoneId.systemDefault());
        return java.util.stream.Stream.iterate(
                        window.firstDate(),
                        date -> date.plusDays(1))
                .limit(42)
                .map(AgendaDay::empty)
                .collect(java.util.stream.Collectors.toList());
    }

    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(new Locale("pt", "BR"))
                + value.substring(1);
    }
}
