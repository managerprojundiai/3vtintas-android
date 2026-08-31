package br.com.tresvtintas.mobile.core.order;

import java.util.Locale;
import java.util.Optional;

public record OrderQuery(
        Optional<String> search,
        Optional<OrderType> type,
        Optional<OrderStatus> status,
        OrderView view,
        int pageSize) {
    public OrderQuery {
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        type = type == null ? Optional.empty() : type;
        status = status == null ? Optional.empty() : status;
        view = view == null ? OrderView.ACTIVE : view;
        if (status.isPresent() && view != OrderView.ALL) {
            throw new IllegalArgumentException(
                    "Status and lifecycle view cannot be combined.");
        }
        if (search.map(String::length).orElse(0) > 80
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException("Order query is invalid.");
        }
    }

    public static OrderQuery initial() {
        return new OrderQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OrderView.ACTIVE,
                30);
    }

    public OrderQuery withSearch(String value) {
        return new OrderQuery(Optional.ofNullable(value), type, status, view, pageSize);
    }

    public OrderQuery withView(OrderView value) {
        return new OrderQuery(search, type, Optional.empty(), value, pageSize);
    }
}
