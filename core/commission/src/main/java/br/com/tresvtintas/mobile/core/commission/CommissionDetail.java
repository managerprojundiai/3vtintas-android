package br.com.tresvtintas.mobile.core.commission;

import java.util.Optional;
import java.util.OptionalLong;

public record CommissionDetail(
        CommissionSummary summary,
        Optional<String> workflowId,
        OptionalLong batchId,
        Optional<Actor> approvedBy,
        Optional<Payment> payment,
        Optional<Cancellation> cancellation) {
    public CommissionDetail {
        if (summary == null) {
            throw new IllegalArgumentException("Commission detail summary is required.");
        }
        workflowId = workflowId == null ? Optional.empty() : workflowId;
        batchId = batchId == null ? OptionalLong.empty() : batchId;
        approvedBy = approvedBy == null ? Optional.empty() : approvedBy;
        payment = payment == null ? Optional.empty() : payment;
        cancellation = cancellation == null ? Optional.empty() : cancellation;
        if (workflowId.filter(String::isBlank).isPresent()
                || (batchId.isPresent() && batchId.orElseThrow() < 1)) {
            throw new IllegalArgumentException("Commission detail is invalid.");
        }
    }

    public record Payment(
            Optional<CommissionPaymentMethod> method,
            Optional<String> reference,
            Optional<Actor> paidBy) {
        public Payment {
            method = method == null ? Optional.empty() : method;
            reference = reference == null ? Optional.empty() : reference;
            paidBy = paidBy == null ? Optional.empty() : paidBy;
            if (reference.filter(String::isBlank).isPresent()) {
                throw new IllegalArgumentException(
                        "Commission payment is invalid.");
            }
        }
    }

    public record Actor(long userId, Optional<String> name) {
        public Actor {
            name = name == null ? Optional.empty() : name;
            if (userId < 1 || name.filter(String::isBlank).isPresent()) {
                throw new IllegalArgumentException("Commission actor is invalid.");
            }
        }
    }

    public record Cancellation(Optional<String> reason, Optional<Actor> cancelledBy) {
        public Cancellation {
            reason = reason == null ? Optional.empty() : reason;
            cancelledBy = cancelledBy == null ? Optional.empty() : cancelledBy;
            if (reason.filter(String::isBlank).isPresent()) {
                throw new IllegalArgumentException("Commission cancellation is invalid.");
            }
        }
    }
}
