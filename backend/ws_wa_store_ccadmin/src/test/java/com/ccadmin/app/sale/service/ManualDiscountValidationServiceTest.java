package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.shared.ProductDiscountPolicy;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManualDiscountValidationServiceTest {

    private final ManualDiscountValidationService service = new ManualDiscountValidationService(
            new ProductDiscountPolicy()
    );

    @Test
    void acceptsPercentageDiscountAtConfiguredLimit() {
        ProductConfigEntity config = discountableConfig("MP", "5.00");

        BigDecimal result = service.validate(
                "P001", new BigDecimal("100.00"), new BigDecimal("5.00"), config, true
        );

        assertEquals(new BigDecimal("5.00"), result);
    }

    @Test
    void rejectsPercentageDiscountAboveConfiguredLimit() {
        ProductConfigEntity config = discountableConfig("MP", "5.00");

        assertThrows(PresaleBuildException.class, () -> service.validate(
                "P001", new BigDecimal("100.00"), new BigDecimal("5.01"), config, true
        ));
    }

    @Test
    void acceptsFixedDiscountAtConfiguredLimit() {
        ProductConfigEntity config = discountableConfig("MF", "12.50");

        BigDecimal result = service.validate(
                "P001", new BigDecimal("100.00"), new BigDecimal("12.50"), config, true
        );

        assertEquals(new BigDecimal("12.50"), result);
    }

    @Test
    void rejectsDiscountWhenCompanyIndicatorIsDisabled() {
        ProductConfigEntity config = discountableConfig("MF", "10.00");

        assertThrows(PresaleBuildException.class, () -> service.validate(
                "P001", new BigDecimal("100.00"), new BigDecimal("1.00"), config, false
        ));
    }

    @Test
    void rejectsDiscountForNonDiscountableProduct() {
        ProductConfigEntity config = discountableConfig("MF", "10.00");
        config.IsDiscontable = "N";

        assertThrows(PresaleBuildException.class, () -> service.validate(
                "P001", new BigDecimal("100.00"), new BigDecimal("1.00"), config, true
        ));
    }

    @Test
    void alwaysAcceptsZeroDiscount() {
        ProductConfigEntity config = discountableConfig("MF", "10.00");
        config.IsDiscontable = "N";

        BigDecimal result = service.validate(
                "P001", new BigDecimal("100.00"), BigDecimal.ZERO, config, false
        );

        assertEquals(new BigDecimal("0.00"), result);
    }

    private ProductConfigEntity discountableConfig(String discountType, String maximum) {
        ProductConfigEntity config = new ProductConfigEntity();
        config.IsDiscontable = "S";
        config.DiscountType = discountType;
        config.NumDiscountMax = new BigDecimal(maximum);
        config.NumPrice = new BigDecimal("100.00");
        return config;
    }
}
