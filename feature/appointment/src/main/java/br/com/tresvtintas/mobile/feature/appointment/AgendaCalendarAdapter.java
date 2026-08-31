package br.com.tresvtintas.mobile.feature.appointment;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.appointment.AgendaDay;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

final class AgendaCalendarAdapter
        extends RecyclerView.Adapter<AgendaCalendarAdapter.Holder> {
    private final Consumer<LocalDate> onSelected;
    private final DateTimeFormatter accessibilityFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(new Locale("pt", "BR"));
    private final NumberFormat integer =
            NumberFormat.getIntegerInstance(new Locale("pt", "BR"));
    private List<AgendaDay> days = List.of();
    private YearMonth month = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    AgendaCalendarAdapter(Consumer<LocalDate> onSelected) {
        this.onSelected = Objects.requireNonNull(
                onSelected,
                "Agenda day listener is required.");
    }

    void submit(
            YearMonth valueMonth,
            LocalDate valueSelectedDate,
            List<AgendaDay> valueDays) {
        YearMonth nextMonth = Objects.requireNonNull(
                valueMonth,
                "Agenda month is required.");
        LocalDate nextSelectedDate = Objects.requireNonNull(
                valueSelectedDate,
                "Agenda selected date is required.");
        if (valueDays == null || valueDays.size() != 42) {
            throw new IllegalArgumentException(
                    "Agenda calendar requires six complete weeks.");
        }
        List<AgendaDay> nextDays = List.copyOf(valueDays);
        List<AgendaDay> previousDays = days;
        YearMonth previousMonth = month;
        LocalDate previousSelectedDate = selectedDate;
        month = nextMonth;
        selectedDate = nextSelectedDate;
        days = nextDays;
        if (previousDays.isEmpty()) {
            notifyItemRangeInserted(0, nextDays.size());
        } else if (!previousMonth.equals(nextMonth)) {
            notifyItemRangeChanged(0, nextDays.size());
        } else {
            notifyChangedDays(
                    previousDays,
                    previousSelectedDate,
                    nextSelectedDate);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(new AgendaDayCellView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private void notifyChangedDays(
            List<AgendaDay> previousDays,
            LocalDate previousSelectedDate,
            LocalDate nextSelectedDate) {
        for (int position = 0; position < days.size(); position++) {
            AgendaDay previous = previousDays.get(position);
            AgendaDay next = days.get(position);
            if (!previous.equals(next)
                    || previous.date().equals(previousSelectedDate)
                            != next.date().equals(nextSelectedDate)) {
                notifyItemChanged(position);
            }
        }
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final AgendaDayCellView cell;

        Holder(AgendaDayCellView cell) {
            super(cell);
            this.cell = cell;
        }

        void bind(AgendaDay day) {
            boolean selected = day.date().equals(selectedDate);
            boolean currentMonth = YearMonth.from(day.date()).equals(month);
            String date = accessibilityFormatter.format(day.date());
            String description = cell.getResources().getQuantityString(
                            R.plurals.agenda_day_accessibility,
                            day.entries().size(),
                            date,
                            day.entries().size());
            cell.bind(
                    integer.format(day.date().getDayOfMonth()),
                    selected,
                    currentMonth,
                    day.markers(),
                    description,
                    ignored -> onSelected.accept(day.date()));
        }
    }
}
