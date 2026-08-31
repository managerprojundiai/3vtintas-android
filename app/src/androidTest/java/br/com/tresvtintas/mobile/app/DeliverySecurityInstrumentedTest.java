package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryDetailActivity;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryListActivity;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryManagementDetailActivity;
import br.com.tresvtintas.mobile.feature.delivery.DeliveryManagementListActivity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class DeliverySecurityInstrumentedTest {
    @Test
    public void deliveryListBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<DeliveryListActivity> scenario =
                     ActivityScenario.launch(intent(DeliveryListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Delivery list must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must not expose delivery rows.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_list)
                                .isShown());
            });
        }
    }

    @Test
    public void deliveryDetailBlocksCaptureAndFailsClosedWithoutRuntime() {
        Context context = targetContext();
        try (ActivityScenario<DeliveryDetailActivity> scenario =
                     ActivityScenario.launch(
                             DeliveryDetailActivity.intent(context, 701))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Delivery detail must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must hide the start action.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_action_start)
                                .isShown());
                assertFalse(
                        "Missing authorization must hide the completion action.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_action_complete)
                                .isShown());
            });
        }
    }

    @Test
    public void managementListBlocksCaptureAndFailsClosedWithoutRuntime() {
        try (ActivityScenario<DeliveryManagementListActivity> scenario =
                     ActivityScenario.launch(
                             intent(DeliveryManagementListActivity.class))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Management list must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must hide managed delivery rows.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_management_list)
                                .isShown());
            });
        }
    }

    @Test
    public void managementDetailBlocksCaptureAndFailsClosedWithoutRuntime() {
        Context context = targetContext();
        try (ActivityScenario<DeliveryManagementDetailActivity> scenario =
                     ActivityScenario.launch(
                             DeliveryManagementDetailActivity.intent(
                                     context,
                                     7,
                                     501))) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "Management detail must set FLAG_SECURE.",
                        secure(activity.getWindow().getAttributes().flags));
                assertFalse(
                        "Missing authorization must hide scheduling.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_management_action_schedule)
                                .isShown());
                assertFalse(
                        "Missing authorization must hide assignment.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_management_action_assign)
                                .isShown());
                assertFalse(
                        "Missing authorization must hide completion.",
                        activity.findViewById(
                                br.com.tresvtintas.mobile.feature.delivery.R.id
                                        .delivery_management_action_complete)
                                .isShown());
            });
        }
    }

    @Test
    public void deliverySliceDeclaresNoLocationOrForegroundServiceCapability()
            throws PackageManager.NameNotFoundException {
        Context context = targetContext();
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_PERMISSIONS);
        List<String> permissions = packageInfo.requestedPermissions == null
                ? Collections.emptyList()
                : Arrays.asList(packageInfo.requestedPermissions);

        assertFalse(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION));
        assertFalse(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION));
        assertFalse(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION));
        assertFalse(permissions.contains(Manifest.permission.FOREGROUND_SERVICE));

        assertNotExported(packageManager, DeliveryListActivity.class);
        assertNotExported(packageManager, DeliveryDetailActivity.class);
        assertNotExported(
                packageManager,
                DeliveryManagementListActivity.class);
        assertNotExported(
                packageManager,
                DeliveryManagementDetailActivity.class);
    }

    private static void assertNotExported(
            PackageManager packageManager,
            Class<?> activityClass) throws PackageManager.NameNotFoundException {
        ActivityInfo activityInfo = packageManager.getActivityInfo(
                new ComponentName(targetContext(), activityClass),
                PackageManager.GET_META_DATA);
        assertFalse(
                activityClass.getSimpleName() + " must not be exported.",
                activityInfo.exported);
    }

    private static <T> Intent intent(Class<T> activityClass) {
        return new Intent(targetContext(), activityClass);
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
    }

    private static boolean secure(int flags) {
        return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }
}
