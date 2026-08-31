package br.com.tresvtintas.mobile.core.network.dto;

public record NotificationCategoriesDto(
        boolean attendance,
        boolean orders,
        boolean deliveries,
        boolean quotes,
        boolean commissions,
        boolean appointments,
        boolean finance,
        boolean agent) {
}
