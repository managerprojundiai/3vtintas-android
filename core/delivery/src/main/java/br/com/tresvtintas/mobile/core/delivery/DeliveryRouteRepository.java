package br.com.tresvtintas.mobile.core.delivery;

import java.time.LocalDate;

public interface DeliveryRouteRepository {
    DeliveryRoutePage routes(LocalDate serviceDate) throws DeliveryException;

    DeliveryRoutePlan route(String routeKey) throws DeliveryException;
}
