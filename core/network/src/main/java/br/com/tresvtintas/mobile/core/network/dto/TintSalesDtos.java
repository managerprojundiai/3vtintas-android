package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.List;

public final class TintSalesDtos {
    private TintSalesDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsSourceSystem(String sourceSystem) {
        return "CORIMO".equals(sourceSystem) || "LKC".equals(sourceSystem);
    }

    public record Configuration(
            String sourceSystem,
            String lineName,
            String finishName,
            String packageName) {
        public Configuration {
            sourceSystem = DtoValidation.requireText(
                    sourceSystem, "Tint source", 16);
            if (!supportsSourceSystem(sourceSystem)) {
                throw new IllegalArgumentException("Tint source is invalid.");
            }
            lineName = DtoValidation.requireText(lineName, "Tint line", 160);
            finishName = DtoValidation.requireText(
                    finishName, "Tint finish", 160);
            packageName = DtoValidation.requireText(
                    packageName, "Tint package", 160);
        }
    }

    public record Pricing(
            long organizationId,
            String currency,
            String priceListPublicId,
            String priceListCode,
            String priceListName,
            String priceListVersionPublicId,
            int priceListVersionNumber,
            int policyRevision,
            String selectionMode) {
        public Pricing {
            organizationId = DtoValidation.requirePositive(
                    organizationId, "Pricing organization ID");
            if (!"BRL".equals(currency)
                    || priceListVersionNumber < 1 || policyRevision < 1
                    || !validSelectionMode(selectionMode)) {
                throw new IllegalArgumentException("Tint pricing metadata is invalid.");
            }
            priceListPublicId = DtoValidation.requireUuid(
                    priceListPublicId, "Price-list ID");
            priceListCode = DtoValidation.requireText(
                    priceListCode, "Price-list code", 80);
            priceListName = DtoValidation.requireText(
                    priceListName, "Price-list name", 160);
            priceListVersionPublicId = DtoValidation.requireUuid(
                    priceListVersionPublicId, "Price-list version ID");
        }
    }

    public record ConfigurationResponse(
            List<Configuration> items,
            Pricing pricing) {
        public ConfigurationResponse {
            if (items == null || items.size() > 500 || pricing == null) {
                throw new IllegalArgumentException(
                        "Tint configuration response is invalid.");
            }
            items = List.copyOf(items);
        }
    }

    public record Color(
            long productId,
            String productName,
            String productSku,
            String productUnit,
            String productBrand,
            long colorId,
            String colorPublicId,
            String colorName,
            String hexColor,
            long tintContextId,
            String tintContextPublicId,
            String sourceSystem,
            String lineName,
            String finishName,
            String packageName,
            String amount,
            String currency) {
        public Color {
            productId = DtoValidation.requirePositive(productId, "Tint product ID");
            productName = DtoValidation.requireText(
                    productName, "Tint product name", 255);
            productSku = optionalNonBlank(productSku, "Tint product SKU", 64);
            productUnit = optionalNonBlank(productUnit, "Tint product unit", 64);
            productBrand = optionalNonBlank(productBrand, "Tint product brand", 160);
            colorId = DtoValidation.requirePositive(colorId, "Tint color ID");
            colorPublicId = DtoValidation.requireUuid(colorPublicId, "Tint color public ID");
            colorName = DtoValidation.requireText(colorName, "Tint color name", 255);
            if (hexColor != null && !hexColor.matches("^#[0-9A-Fa-f]{6}$")) {
                throw new IllegalArgumentException("Tint hexadecimal color is invalid.");
            }
            tintContextId = DtoValidation.requirePositive(
                    tintContextId, "Tint context ID");
            tintContextPublicId = DtoValidation.requireUuid(
                    tintContextPublicId, "Tint context public ID");
            new Configuration(sourceSystem, lineName, finishName, packageName);
            if (!"BRL".equals(currency) || amount == null
                    || !amount.matches("^(?:0|[1-9]\\d{0,9})\\.\\d{2}$")
                    || new BigDecimal(amount).signum() < 0) {
                throw new IllegalArgumentException("Tint amount is invalid.");
            }
        }
    }

    public record ColorResponse(
            List<Color> items,
            boolean hasMore,
            Pricing pricing) {
        public ColorResponse {
            if (items == null || items.size() > 30 || pricing == null) {
                throw new IllegalArgumentException("Tint color response is invalid.");
            }
            items = List.copyOf(items);
        }
    }

    private static boolean validSelectionMode(String value) {
        return "PRIMARY".equals(value)
                || "EXPLICIT_ACTIVE".equals(value)
                || "MANAGER_OVERRIDE".equals(value);
    }

    private static String optionalNonBlank(
            String value,
            String field,
            int maximumLength) {
        if (value == null) {
            return null;
        }
        return DtoValidation.requireText(value, field, maximumLength);
    }
}
