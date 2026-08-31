package br.com.tresvtintas.mobile.feature.delivery;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStop;
import java.util.Locale;

final class DeliveryRouteNavigator {
    private DeliveryRouteNavigator() {
        throw new AssertionError("No instances.");
    }

    static void open(Activity activity, DeliveryRouteStop stop) {
        String coordinates = String.format(
                Locale.ROOT,
                "%.7f,%.7f",
                stop.coordinate().latitude(),
                stop.coordinate().longitude());
        Uri destination = Uri.parse("geo:" + coordinates)
                .buildUpon()
                .appendQueryParameter("q", coordinates + " (" + stop.address() + ")")
                .build();
        Intent navigation = new Intent(Intent.ACTION_VIEW, destination);
        Intent chooser = Intent.createChooser(
                navigation,
                activity.getString(R.string.delivery_route_navigation_title));
        try {
            activity.startActivity(chooser);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    activity,
                    R.string.delivery_route_navigation_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }
}
