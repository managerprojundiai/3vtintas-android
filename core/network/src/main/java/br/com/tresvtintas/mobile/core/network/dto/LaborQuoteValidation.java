package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

final class LaborQuoteValidation {
    static final Set<String> STATUSES = Set.of(
            "draft", "sent", "accepted", "converted", "rejected", "expired");
    static final Set<String> MANUAL_STATUSES = Set.of(
            "draft", "sent", "accepted", "rejected", "expired");

    private LaborQuoteValidation() {
    }
}
