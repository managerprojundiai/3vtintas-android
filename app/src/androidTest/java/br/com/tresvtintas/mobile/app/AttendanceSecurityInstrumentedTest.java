package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceDetailActivity;
import br.com.tresvtintas.mobile.feature.attendance.AttendanceListActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AttendanceSecurityInstrumentedTest {
    @Test
    public void attendanceListBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<AttendanceListActivity> scenario =
                     ActivityScenario.launch(
                             new Intent(
                                     targetContext(),
                                     AttendanceListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Attendance list must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must not expose conversation rows.",
                        activity.findViewById(
                                        br.com.tresvtintas.mobile.feature.attendance.R.id
                                                .attendance_list)
                                .isShown());
            });
        }
    }

    @Test
    public void attendanceActivityIsNotExported()
            throws PackageManager.NameNotFoundException {
        Context context = targetContext();
        ActivityInfo listInfo = context.getPackageManager()
                .getActivityInfo(
                        new ComponentName(
                                context,
                                AttendanceListActivity.class),
                        PackageManager.GET_META_DATA);

        assertFalse(
                "Attendance list must not be exported.",
                listInfo.exported);
        ActivityInfo detailInfo = context.getPackageManager()
                .getActivityInfo(
                        new ComponentName(
                                context,
                                AttendanceDetailActivity.class),
                        PackageManager.GET_META_DATA);
        assertFalse(
                "Attendance detail must not be exported.",
                detailInfo.exported);
    }

    @Test
    public void attendanceDetailBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<AttendanceDetailActivity> scenario =
                     ActivityScenario.launch(
                             new Intent(
                                     targetContext(),
                                     AttendanceDetailActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Attendance detail must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must not expose messages.",
                        activity.findViewById(
                                        br.com.tresvtintas.mobile.feature.attendance.R.id
                                                .attendance_detail_messages)
                                .isShown());
                assertFalse(
                        "Missing reply authorization must hide the composer.",
                        activity.findViewById(
                                        br.com.tresvtintas.mobile.feature.attendance.R.id
                                                .attendance_reply_group)
                                .isShown());
                assertFalse(
                        "Missing management authorization must hide controls.",
                        activity.findViewById(
                                        br.com.tresvtintas.mobile.feature.attendance.R.id
                                                .attendance_management_group)
                                .isShown());
                assertFalse(
                        "Reply drafts must not enter Android saved state.",
                        activity.findViewById(
                                        br.com.tresvtintas.mobile.feature.attendance.R.id
                                                .attendance_reply_input)
                                .isSaveEnabled());
            });
        }
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
    }

    private static boolean secure(int flags) {
        return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }
}
