package com.ccadmin.app.inventory.model.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StockMovementPriceCalculator {

    private static final int MONEY_SCALE = 2;

    private StockMovementPriceCalculator() {
    }

    public static PriceAmounts normalize(
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
        int normalizedQuantity = quantity == null ? 0 : quantity;
        BigDecimal normalizedUnitPrice = nonNegative(unitPrice, "El precio unitario");
        BigDecimal normalizedTotalPrice = nonNegative(totalPrice, "El precio total");

        if (normalizedTotalPrice.signum() == 0
                && normalizedUnitPrice.signum() > 0) {
            normalizedTotalPrice = normalizedUnitPrice
                    .multiply(BigDecimal.valueOf(normalizedQuantity))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        if (normalizedQuantity > 0) {
            normalizedUnitPrice = normalizedTotalPrice
                    .divide(
                            BigDecimal.valueOf(normalizedQuantity),
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );
        }

        return new PriceAmounts(normalizedUnitPrice, normalizedTotalPrice);
    }

    public static BigDecimal add(BigDecimal total, BigDecimal amount) {
        return value(total).add(value(amount)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        BigDecimal amount = value(value);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " no puede ser negativo");
        }
        return amount;
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record PriceAmounts(BigDecimal unitPrice, BigDecimal totalPrice) {
    }
}
