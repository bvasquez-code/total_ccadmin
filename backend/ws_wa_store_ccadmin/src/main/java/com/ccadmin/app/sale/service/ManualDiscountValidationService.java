package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.shared.ProductDiscountPolicy;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ManualDiscountValidationService {

    private final ProductDiscountPolicy productDiscountPolicy;

    public ManualDiscountValidationService(ProductDiscountPolicy productDiscountPolicy) {
        this.productDiscountPolicy = productDiscountPolicy;
    }

    public BigDecimal validate(
            String productCod,
            BigDecimal unitPrice,
            BigDecimal unitDiscount,
            ProductConfigEntity config,
            boolean manualDiscountEnabled
    ) throws PresaleBuildException {
        BigDecimal discount = money(unitDiscount);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new PresaleBuildException("El descuento del producto " + productCod + " no puede ser negativo");
        }
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            return discount;
        }
        if (!manualDiscountEnabled) {
            throw new PresaleBuildException("El descuento manual no esta habilitado para esta empresa");
        }
        if (config == null || !"S".equalsIgnoreCase(clean(config.IsDiscontable))) {
            throw new PresaleBuildException("El producto " + productCod + " no permite descuentos manuales");
        }

        BigDecimal price = money(unitPrice);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PresaleBuildException("El producto " + productCod + " no tiene un precio valido para aplicar descuento");
        }
        if (discount.compareTo(price) > 0) {
            throw new PresaleBuildException("El descuento del producto " + productCod + " no puede superar su precio");
        }

        BigDecimal allowedDiscount;
        try {
            allowedDiscount = this.productDiscountPolicy.calculateMaximumUnitDiscount(config, price);
        } catch (IllegalArgumentException exception) {
            throw new PresaleBuildException(
                    "La configuracion de descuento del producto " + productCod + " no es valida: "
                            + exception.getMessage()
            );
        }

        if (discount.compareTo(allowedDiscount) > 0) {
            String limit = ProductDiscountPolicy.PERCENTAGE_TYPE.equals(config.DiscountType)
                    ? config.NumDiscountMax.stripTrailingZeros().toPlainString() + "%"
                    : allowedDiscount.toPlainString();
            throw new PresaleBuildException(
                    "El descuento del producto " + productCod + " supera el limite configurado de " + limit
            );
        }
        return discount;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
