package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class LaborQuoteEditorModel {
    private static final int MAXIMUM_ITEMS = 100;
    private final List<LaborQuoteDraftLine> lines = new ArrayList<>();

    LaborQuoteEditorModel() {
    }

    LaborQuoteEditorModel(List<LaborQuoteDraftLine> restored) {
        if (restored != null) {
            lines.addAll(restored);
        }
    }

    List<LaborQuoteDraftLine> lines() {
        return List.copyOf(lines);
    }

    void add(LaborQuoteDraftLine line) {
        if (lines.size() >= MAXIMUM_ITEMS) {
            throw new IllegalArgumentException("Labor quote item limit reached.");
        }
        lines.add(line);
    }

    void replace(int index, LaborQuoteDraftLine line) {
        lines.set(index, line);
    }

    void remove(int index) {
        lines.remove(index);
    }

    BigDecimal subtotal() {
        return lines.stream()
                .map(LaborQuoteDraftLine::total)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    BigDecimal total(BigDecimal discount) {
        BigDecimal normalized = discount == null ? BigDecimal.ZERO : discount;
        return subtotal().subtract(normalized).max(BigDecimal.ZERO).setScale(2);
    }
}
