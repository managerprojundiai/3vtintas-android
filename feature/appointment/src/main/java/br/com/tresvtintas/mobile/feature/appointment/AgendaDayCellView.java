package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.appointment.AgendaEntryType;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import java.util.Set;

final class AgendaDayCellView extends MaterialCardView {
    private static final float NUMBER_CENTER_WITH_MARKERS = 0.36f;
    private static final float NUMBER_CENTER_WITHOUT_MARKERS = 0.50f;
    private static final float MARKER_CENTER = 0.72f;
    private static final int MARKER_COUNT = 5;
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean[] visibleMarkers = new boolean[MARKER_COUNT];
    private final int[] markerColors = new int[MARKER_COUNT];
    private final int surfaceColor;
    private final int selectedSurfaceColor;
    private final int strokeColor;
    private final int textColor;
    private final int selectedTextColor;
    private final float markerRadius;
    private final float markerSpacing;
    private final int selectedStrokeWidth;
    private String dayNumber = "";
    private boolean selected;

    AgendaDayCellView(Context context) {
        this(context, null);
    }

    AgendaDayCellView(Context context, AttributeSet attributes) {
        this(context, attributes, com.google.android.material.R.attr
                .materialCardViewStyle);
    }

    AgendaDayCellView(
            Context context,
            AttributeSet attributes,
            int defaultStyle) {
        super(context, attributes, defaultStyle);
        int height = getResources().getDimensionPixelSize(
                R.dimen.agenda_day_cell_height);
        int margin = getResources().getDimensionPixelSize(
                R.dimen.agenda_day_cell_margin);
        RecyclerView.LayoutParams parameters = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height);
        parameters.setMargins(margin, margin, margin, margin);
        setLayoutParams(parameters);
        setRadius(getResources().getDimension(
                R.dimen.agenda_day_cell_radius));
        setCardElevation(0.0f);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setWillNotDraw(false);

        surfaceColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurface);
        selectedSurfaceColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimaryContainer);
        strokeColor = MaterialColors.getColor(
                this,
                androidx.appcompat.R.attr.colorPrimary);
        textColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface);
        selectedTextColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnPrimaryContainer);
        selectedStrokeWidth = getResources().getDimensionPixelSize(
                R.dimen.agenda_day_selected_stroke);
        float markerSize = getResources().getDimension(
                R.dimen.agenda_marker_size);
        markerRadius = markerSize / 2.0f;
        markerSpacing = markerSize + 2.0f * getResources().getDimension(
                R.dimen.agenda_marker_margin);

        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTextSize(getResources().getDimension(
                R.dimen.agenda_day_text_size));
        numberPaint.setTypeface(Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL));
        markerColors[0] = color(R.color.agenda_marker_appointment_color);
        markerColors[1] = color(R.color.agenda_marker_delivery_color);
        markerColors[2] = color(R.color.agenda_marker_receivable_color);
        markerColors[3] = color(R.color.agenda_marker_payable_color);
        markerColors[4] = color(R.color.agenda_marker_expense_color);
    }

    void bind(
            String number,
            boolean valueSelected,
            boolean currentMonth,
            Set<AgendaEntryType> markers,
            String description,
            OnClickListener listener) {
        dayNumber = number;
        selected = valueSelected;
        setSelected(valueSelected);
        visibleMarkers[0] = markers.contains(AgendaEntryType.APPOINTMENT)
                || markers.contains(AgendaEntryType.COLLECTION);
        visibleMarkers[1] = markers.contains(AgendaEntryType.DELIVERY);
        visibleMarkers[2] = markers.contains(AgendaEntryType.RECEIVABLE);
        visibleMarkers[3] = markers.contains(AgendaEntryType.PAYABLE);
        visibleMarkers[4] = markers.contains(AgendaEntryType.EXPENSE);
        setStrokeWidth(selected ? selectedStrokeWidth : 0);
        setStrokeColor(strokeColor);
        setCardBackgroundColor(
                selected ? selectedSurfaceColor : surfaceColor);
        setAlpha(currentMonth ? 1.0f : 0.42f);
        setContentDescription(description);
        setOnClickListener(listener);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int markerTotal = markerTotal();
        float numberCenter = markerTotal == 0
                ? NUMBER_CENTER_WITHOUT_MARKERS
                : NUMBER_CENTER_WITH_MARKERS;
        Paint.FontMetrics metrics = numberPaint.getFontMetrics();
        float baseline = getHeight() * numberCenter
                - (metrics.ascent + metrics.descent) / 2.0f;
        numberPaint.setColor(selected ? selectedTextColor : textColor);
        canvas.drawText(dayNumber, getWidth() / 2.0f, baseline, numberPaint);
        drawMarkers(canvas, markerTotal);
    }

    private void drawMarkers(Canvas canvas, int markerTotal) {
        if (markerTotal == 0) {
            return;
        }
        float firstCenter = getWidth() / 2.0f
                - (markerTotal - 1) * markerSpacing / 2.0f;
        int visibleIndex = 0;
        for (int index = 0; index < MARKER_COUNT; index++) {
            if (!visibleMarkers[index]) {
                continue;
            }
            markerPaint.setColor(markerColors[index]);
            canvas.drawCircle(
                    firstCenter + visibleIndex * markerSpacing,
                    getHeight() * MARKER_CENTER,
                    markerRadius,
                    markerPaint);
            visibleIndex += 1;
        }
    }

    private int markerTotal() {
        int result = 0;
        for (boolean visible : visibleMarkers) {
            if (visible) {
                result += 1;
            }
        }
        return result;
    }

    private int color(int resource) {
        return ContextCompat.getColor(getContext(), resource);
    }
}
