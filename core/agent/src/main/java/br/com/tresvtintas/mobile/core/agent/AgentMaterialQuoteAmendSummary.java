package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AgentMaterialQuoteAmendSummary(
        long quoteId,
        String quoteTitle,
        String customerName,
        int expectedRevision,
        List<AgentMaterialQuoteAmendChange> changes,
        AgentMaterialQuoteAmendSnapshot before,
        AgentMaterialQuoteAmendSnapshot after,
        boolean pricingWillBeRevalidated)
        implements AgentActionSummary {
    private static final int MAXIMUM_CHANGES = 100;

    public AgentMaterialQuoteAmendSummary {
        if (quoteId < 1 || expectedRevision < 1) {
            throw new IllegalArgumentException(
                    "Agent amendment identity is invalid.");
        }
        quoteTitle = requireText(quoteTitle);
        customerName = requireText(customerName);
        changes = List.copyOf(Objects.requireNonNull(
                changes,
                "Agent amendment changes are required."));
        before = Objects.requireNonNull(
                before,
                "Agent amendment before snapshot is required.");
        after = Objects.requireNonNull(
                after,
                "Agent amendment after snapshot is required.");
        if (changes.isEmpty()
                || changes.size() > MAXIMUM_CHANGES
                || !pricingWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Agent amendment summary is invalid.");
        }
        validateChanges(changes, before, after);
    }

    @Override
    public List<AgentMaterialQuoteAmendChange> changes() {
        return List.copyOf(changes);
    }

    public BigDecimal total() {
        return after.total();
    }

    private static void validateChanges(
            List<AgentMaterialQuoteAmendChange> changes,
            AgentMaterialQuoteAmendSnapshot before,
            AgentMaterialQuoteAmendSnapshot after) {
        Map<Long, AgentMaterialQuoteAmendItem> previous =
                byProduct(before.items());
        Map<Long, AgentMaterialQuoteAmendItem> next =
                byProduct(after.items());
        Set<Long> changedProducts = new HashSet<>();
        Set<Long> expectedProducts = new HashSet<>();
        Set<Long> allProducts = new HashSet<>(previous.keySet());
        allProducts.addAll(next.keySet());
        for (long productId : allProducts) {
            AgentMaterialQuoteAmendItem oldItem =
                    previous.get(productId);
            AgentMaterialQuoteAmendItem newItem = next.get(productId);
            if (oldItem == null
                    || newItem == null
                    || oldItem.quantity().compareTo(
                                    newItem.quantity())
                            != 0) {
                expectedProducts.add(productId);
            }
        }
        for (AgentMaterialQuoteAmendChange change : changes) {
            if (!changedProducts.add(change.productId())
                    || !matches(change, previous, next)) {
                throw new IllegalArgumentException(
                        "Agent amendment changes do not match snapshots.");
            }
        }
        if (!changedProducts.equals(expectedProducts)) {
            throw new IllegalArgumentException(
                    "Agent amendment changes are incomplete.");
        }
    }

    private static boolean matches(
            AgentMaterialQuoteAmendChange change,
            Map<Long, AgentMaterialQuoteAmendItem> before,
            Map<Long, AgentMaterialQuoteAmendItem> after) {
        AgentMaterialQuoteAmendItem oldItem =
                before.get(change.productId());
        AgentMaterialQuoteAmendItem newItem =
                after.get(change.productId());
        return switch (change.operation()) {
            case ADD -> oldItem == null
                    && newItem != null
                    && change.description().equals(
                            newItem.description())
                    && change.afterQuantity().orElseThrow()
                            .compareTo(newItem.quantity())
                            == 0;
            case UPDATE_QUANTITY -> oldItem != null
                    && newItem != null
                    && change.description().equals(
                            newItem.description())
                    && change.beforeQuantity().orElseThrow()
                            .compareTo(oldItem.quantity())
                            == 0
                    && change.afterQuantity().orElseThrow()
                            .compareTo(newItem.quantity())
                            == 0;
            case REMOVE -> oldItem != null
                    && newItem == null
                    && change.description().equals(
                            oldItem.description())
                    && change.beforeQuantity().orElseThrow()
                            .compareTo(oldItem.quantity())
                            == 0;
        };
    }

    private static Map<Long, AgentMaterialQuoteAmendItem> byProduct(
            List<AgentMaterialQuoteAmendItem> items) {
        Map<Long, AgentMaterialQuoteAmendItem> result = new HashMap<>();
        for (AgentMaterialQuoteAmendItem item : items) {
            result.put(item.productId(), item);
        }
        return result;
    }

    private static String requireText(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 200) {
            throw new IllegalArgumentException(
                    "Agent amendment text is invalid.");
        }
        return value;
    }
}
