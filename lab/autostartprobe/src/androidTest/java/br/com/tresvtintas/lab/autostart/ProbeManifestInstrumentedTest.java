package br.com.tresvtintas.lab.autostart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public final class ProbeManifestInstrumentedTest {
    private static final String BIND_JOB_SERVICE =
            "android.permission.BIND_JOB_SERVICE";

    @Test
    public void labDeclaresOnlyTheCapabilitiesNeededForThePlatformProbe()
            throws PackageManager.NameNotFoundException {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager manager = context.getPackageManager();
        PackageInfo info = manager.getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_PERMISSIONS);
        List<String> permissions = info.requestedPermissions == null
                ? Collections.emptyList()
                : Arrays.asList(info.requestedPermissions);

        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION));
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION));
        assertTrue(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION));
        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE));
        assertTrue(permissions.contains(Manifest.permission.RECEIVE_BOOT_COMPLETED));
    }

    @Test
    public void probeComponentsAreNotExternallyCallable()
            throws PackageManager.NameNotFoundException {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager manager = context.getPackageManager();

        assertFalse(manager.getReceiverInfo(
                new ComponentName(context, ProbeBootReceiver.class),
                PackageManager.GET_META_DATA).exported);
        ServiceInfo service = manager.getServiceInfo(
                new ComponentName(context, ProbeForegroundService.class),
                PackageManager.GET_META_DATA);
        assertFalse(service.exported);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue((service.getForegroundServiceType()
                    & ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) != 0);
        }
        ServiceInfo job = manager.getServiceInfo(
                new ComponentName(context, ProbeJobService.class),
                PackageManager.GET_META_DATA);
        assertFalse(job.exported);
        assertEquals(BIND_JOB_SERVICE, job.permission);
    }

    @Test
    public void stateStoreContainsNoCoordinateFields() {
        Context context = ApplicationProvider.getApplicationContext();
        ProbeStateStore.reset(context);
        ProbeStateStore.Snapshot snapshot = ProbeStateStore.read(context);

        assertTrue(snapshot.attemptedAtElapsedMillis() == 0);
        assertTrue(snapshot.startedAtElapsedMillis() == 0);
        assertTrue(snapshot.stoppedAtElapsedMillis() == 0);
        assertTrue(snapshot.bootAtElapsedMillis() == 0);
    }
}
