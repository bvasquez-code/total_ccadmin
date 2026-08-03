package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductDiscountPolicyTest {

    private final ProductDiscountPolicy policy = new ProductDiscountPolicy();

    @Test
    void normalizesDisabledDiscountConfiguration() {
        ProductConfigEntity config = config("N", "MP", "25.00", "100.00");

        policy.normalizeAndValidate(config);

        assertEquals("-", config.DiscountType);
        assertEquals(new BigDecimal("0.00"), config.NumDiscountMax);
    }

    @Test
    void validatesPercentageMaximum() {
        ProductConfigEntity config = config("S", "MP", "100.01", "100.00");

        assertThrows(IllegalArgumentException.class, () -> policy.normalizeAndValidate(config));
    }

    @Test
    void validatesFixedMaximumAgainstProductPrice() {
        ProductConfigEntity config = config("S", "MF", "100.01", "100.00");

        assertThrows(IllegalArgumentException.class, () -> policy.normalizeAndValidate(config));
    }

    @Test
    void calculatesPercentageMaximumAsUnitAmount() {
        ProductConfigEntity config = config("S", "MP", "5.00", "100.00");

        BigDecimal maximum = policy.calculateMaximumUnitDiscount(config, new BigDecimal("80.00"));

        assertEquals(new BigDecimal("4.00"), maximum);
    }

    private ProductConfigEntity config(
            String isDiscountable,
            String discountType,
            String maximum,
            String price
    ) {
        ProductConfigEntity config = new ProductConfigEntity();
        config.IsDiscontable = isDiscountable;
        config.DiscountType = discountType;
        config.NumDiscountMax = new BigDecimal(maximum);
        config.NumPrice = new BigDecimal(price);
        return config;
    }
}
