package com.ccadmin.app.inventory.model.calculation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockMovementPriceCalculatorTest {

    @Test
    void calculatesTotalFromUnitPriceWhenTotalIsEmpty() {
        var result = StockMovementPriceCalculator.normalize(
                3, new BigDecimal("12.50"), BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("12.50"), result.unitPrice());
        assertEquals(new BigDecimal("37.50"), result.totalPrice());
    }

    @Test
    void calculatesUnitPriceFromEditedTotal() {
        var result = StockMovementPriceCalculator.normalize(
                4, BigDecimal.ZERO, new BigDecimal("25.00")
        );

        assertEquals(new BigDecimal("6.25"), result.unitPrice());
        assertEquals(new BigDecimal("25.00"), result.totalPrice());
    }

    @Test
    void preservesEditedTotalWhenDivisionRequiresRounding() {
        var result = StockMovementPriceCalculator.normalize(
                3, BigDecimal.ZERO, new BigDecimal("10.00")
        );

        assertEquals(new BigDecimal("3.33"), result.unitPrice());
        assertEquals(new BigDecimal("10.00"), result.totalPrice());
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StockMovementPriceCalculator.normalize(
                        1, new BigDecimal("-0.01"), BigDecimal.ZERO
                )
        );
    }
}
