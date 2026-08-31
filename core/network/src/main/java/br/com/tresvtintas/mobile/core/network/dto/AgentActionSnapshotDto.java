package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

public record AgentActionSnapshotDto(
        int itemCount,
        List<AgentActionItemDto> items,
        String subtotal,
        String discount,
        String total) {
    public AgentActionSnapshotDto {
        items = items == null ? List.of() : List.copyOf(items);
        if (itemCount < 1
                || itemCount > 100
                || items.size() != itemCount
                || new HashSet<>(items.stream()
                        .map(AgentActionItemDto::productId)
                        .toList()).size()
                        != items.size()
                || !validMoney(subtotal)
                || !validMoney(discount)
                || !validMoney(total)) {
            throw new IllegalArgumentException(
                    "Agent amendment snapshot is invalid.");
        }
        BigDecimal lines = items.stream()
                .map(item -> new BigDecimal(item.total()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotalAmount = new BigDecimal(subtotal);
        BigDecimal discountAmount = new BigDecimal(discount);
        if (lines.compareTo(subtotalAmount) != 0
                || discountAmount.compareTo(subtotalAmount) > 0
                || subtotalAmount.subtract(discountAmount)
                                .compareTo(new BigDecimal(total))
                        != 0) {
            throw new IllegalArgumentException(
                    "Agent amendment snapshot totals are invalid.");
        }
    }

    @Override
    public List<AgentActionItemDto> items() {
        return List.copyOf(items);
    }

    private static boolean validMoney(String value) {
        return value != null
                && value.matches("^\\d{1,8}\\.\\d{2}$");
    }
}
