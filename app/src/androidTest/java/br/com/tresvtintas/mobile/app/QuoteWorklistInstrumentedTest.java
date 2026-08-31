package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.laborquote.LaborQuoteListActivity;
import br.com.tresvtintas.mobile.feature.quote.MaterialQuoteListActivity;
import com.google.android.material.button.MaterialButton;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class QuoteWorklistInstrumentedTest {
    @Test
    public void materialWorklistStartsOnPendingAndExposesHistory() {
        try (ActivityScenario<MaterialQuoteListActivity> scenario =
                     ActivityScenario.launch(intent(MaterialQuoteListActivity.class))) {
            scenario.onActivity(activity -> {
                MaterialButton pending = activity.findViewById(
                        br.com.tresvtintas.mobile.feature.quote.R.id
                                .quote_view_active);
                MaterialButton history = activity.findViewById(
                        br.com.tresvtintas.mobile.feature.quote.R.id
                                .quote_view_history);
                assertTrue("Pending material work must be the default.",
                        pending.isChecked());
                assertTrue("Historical material work must remain reachable.",
                        history.isShown());
            });
        }
    }

    @Test
    public void laborWorklistStartsOnPendingAndExposesHistory() {
        try (ActivityScenario<LaborQuoteListActivity> scenario =
                     ActivityScenario.launch(intent(LaborQuoteListActivity.class))) {
            scenario.onActivity(activity -> {
                MaterialButton pending = activity.findViewById(
                        br.com.tresvtintas.mobile.feature.laborquote.R.id
                                .labor_quote_view_active);
                MaterialButton history = activity.findViewById(
                        br.com.tresvtintas.mobile.feature.laborquote.R.id
                                .labor_quote_view_history);
                assertTrue("Pending labor work must be the default.",
                        pending.isChecked());
                assertTrue("Historical labor work must remain reachable.",
                        history.isShown());
            });
        }
    }

    private static <T> Intent intent(Class<T> activityClass) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new Intent(context, activityClass);
    }
}
