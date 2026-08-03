package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductDiscountPolicy {

    public static final String PERCENTAGE_TYPE = "MP";
    public static final String FIXED_AMOUNT_TYPE = "MF";

    public ProductConfigEntity normalizeAndValidate(ProductConfigEntity config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuracion de producto requerida");
        }

        config.IsDiscontable = clean(config.IsDiscontable).toUpperCase();
        if (config.IsDiscontable.isEmpty()) {
            config.IsDiscontable = "N";
        }
        if ("N".equals(config.IsDiscontable)) {
            config.DiscountType = "-";
            config.NumDiscountMax = BigDecimal.ZERO.setScale(2);
            return config;
        }
        if (!"S".equals(config.IsDiscontable)) {
            throw new IllegalArgumentException("IsDiscontable debe ser S o N");
        }

        config.DiscountType = clean(config.DiscountType).toUpperCase();
        config.NumDiscountMax = money(config.NumDiscountMax);
        if (!PERCENTAGE_TYPE.equals(config.DiscountType)
                && !FIXED_AMOUNT_TYPE.equals(config.DiscountType)) {
            throw new IllegalArgumentException("DiscountType debe ser MP o MF");
        }
        if (config.NumDiscountMax.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("NumDiscountMax debe ser mayor a cero");
        }
        if (PERCENTAGE_TYPE.equals(config.DiscountType)
                && config.NumDiscountMax.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El porcentaje maximo de descuento no puede superar 100%");
        }
        if (FIXED_AMOUNT_TYPE.equals(config.DiscountType)) {
            BigDecimal configuredPrice = money(config.NumPrice);
            if (configuredPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Debe configurar un precio mayor a cero para usar descuento fijo");
            }
            if (config.NumDiscountMax.compareTo(configuredPrice) > 0) {
                throw new IllegalArgumentException("El descuento fijo maximo no puede superar el precio del producto");
            }
        }
        return config;
    }

    public BigDecimal calculateMaximumUnitDiscount(ProductConfigEntity config, BigDecimal unitPrice) {
        normalizeAndValidate(config);
        BigDecimal price = money(unitPrice);
        if (PERCENTAGE_TYPE.equals(config.DiscountType)) {
            return price.multiply(config.NumDiscountMax)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return config.NumDiscountMax.min(price);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
