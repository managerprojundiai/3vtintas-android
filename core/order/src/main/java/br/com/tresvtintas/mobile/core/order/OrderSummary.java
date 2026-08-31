package br.com.tresvtintas.mobile.core.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public record OrderSummary(
        long id,
        OrderType type,
        OrderStatus status,
        int revision,
        OrderPaymentStatus paymentStatus,
        Set<OrderAction> allowedActions,
        Optional<BigDecimal> total,
        int itemCount,
        OptionalLong quoteId,
        Optional<Organization> organization,
        Optional<Seller> seller,
        Optional<Customer> customer,
        Optional<Delivery> delivery,
        Instant createdAt,
        Instant updatedAt) {
    public OrderSummary {
        if (id < 1
                || type == null
                || status == null
                || revision < 1
                || paymentStatus == null
                || allowedActions == null
                || allowedActions.stream().anyMatch(java.util.Objects::isNull)
                || total == null
                || total.filter(value -> value.signum() < 0).isPresent()
                || itemCount < 0
                || createdAt == null
                || updatedAt == null) {
            throw new IllegalArgumentException("Order summary is invalid.");
        }
        quoteId = quoteId == null ? OptionalLong.empty() : quoteId;
        organization = organization == null ? Optional.empty() : organization;
        seller = seller == null ? Optional.empty() : seller;
        customer = customer == null ? Optional.empty() : customer;
        delivery = delivery == null ? Optional.empty() : delivery;
        allowedActions = Set.copyOf(allowedActions);
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Order organization is invalid.");
            }
        }
    }

    public record Seller(long userId, String role, Optional<String> name) {
        public Seller {
            if (userId < 1
                    || (!"painter".equals(role) && !"salesperson".equals(role))) {
                throw new IllegalArgumentException("Order seller is invalid.");
            }
            name = name == null ? Optional.empty() : name;
        }
    }

    public record Customer(long id, String name) {
        public Customer {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Order customer is invalid.");
            }
        }
    }

    public record Delivery(
            long id,
            OrderDeliveryStatus status,
            Optional<Instant> estimatedAt,
            boolean assignedToCurrentActor) {
        public Delivery {
            if (id < 1 || status == null) {
                throw new IllegalArgumentException("Order delivery is invalid.");
            }
            estimatedAt = estimatedAt == null ? Optional.empty() : estimatedAt;
        }
    }
}
