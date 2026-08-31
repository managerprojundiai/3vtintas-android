package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;

final class LaborQuoteUi {
    private LaborQuoteUi() {
    }

    static int statusLabel(LaborQuoteStatus status) {
        return switch (status) {
            case DRAFT -> R.string.labor_quote_status_draft;
            case SENT -> R.string.labor_quote_status_sent;
            case ACCEPTED -> R.string.labor_quote_status_accepted;
            case CONVERTED -> R.string.labor_quote_status_converted;
            case REJECTED -> R.string.labor_quote_status_rejected;
            case EXPIRED -> R.string.labor_quote_status_expired;
        };
    }
}
