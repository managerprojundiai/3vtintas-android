package br.com.tresvtintas.mobile.core.delivery;

public record DeliveryRouteCoordinate(
        int latitudeE7,
        int longitudeE7) {
    private static final int LATITUDE_LIMIT_E7 = 900_000_000;
    private static final int LONGITUDE_LIMIT_E7 = 1_800_000_000;

    public DeliveryRouteCoordinate {
        if (Math.abs((long) latitudeE7) > LATITUDE_LIMIT_E7
                || Math.abs((long) longitudeE7) > LONGITUDE_LIMIT_E7) {
            throw new IllegalArgumentException("Delivery route coordinate is invalid.");
        }
    }

    public double latitude() {
        return latitudeE7 / 10_000_000.0;
    }

    public double longitude() {
        return longitudeE7 / 10_000_000.0;
    }
}
