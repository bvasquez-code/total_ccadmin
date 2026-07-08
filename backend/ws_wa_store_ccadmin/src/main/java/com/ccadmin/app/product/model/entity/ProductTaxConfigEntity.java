package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "product_tax_config")
public class ProductTaxConfigEntity extends AuditTableEntity implements Serializable {

    private static final BigDecimal STANDARD_IGV_RATE = new BigDecimal("18.0000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long ProductTaxConfigId;
    public String ProductCod;
    public String StoreCod;
    public String TaxCod;
    public String TaxAffectationCod;
    public String IsMainTax;
    public BigDecimal TaxRateValue;
    public BigDecimal FixedUnitAmount;
    public String TaxCalculationType;
    public String IsInformative;
    public int CalculationOrder;

    public ProductTaxConfigEntity validate() {
        if (ProductCod == null || ProductCod.isBlank()) {
            throw new IllegalArgumentException("Producto requerido");
        }
        if (StoreCod == null || StoreCod.isBlank()) {
            throw new IllegalArgumentException("Tienda requerida");
        }
        if (TaxCod == null || TaxCod.isBlank()) {
            throw new IllegalArgumentException("Tributo requerido");
        }
        if (IsMainTax == null || IsMainTax.isBlank()) {
            IsMainTax = "N";
        }
        if (!IsMainTax.equals("S") && !IsMainTax.equals("N")) {
            throw new IllegalArgumentException("Indicador de tributo principal no valido");
        }
        if (IsMainTax.equals("S") && (TaxAffectationCod == null || TaxAffectationCod.isBlank())) {
            throw new IllegalArgumentException("La configuracion principal requiere afectacion tributaria");
        }
        if (TaxCalculationType == null || TaxCalculationType.isBlank()) {
            TaxCalculationType = "P";
        }
        if (!TaxCalculationType.equals("P") && !TaxCalculationType.equals("F") && !TaxCalculationType.equals("N")) {
            throw new IllegalArgumentException("Tipo de calculo tributario no valido");
        }
        if (IsInformative == null || IsInformative.isBlank()) {
            IsInformative = "N";
        }
        if (!IsInformative.equals("S") && !IsInformative.equals("N")) {
            throw new IllegalArgumentException("Indicador informativo no valido");
        }
        if (TaxRateValue != null && TaxRateValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tasa tributaria no puede ser negativa");
        }
        if (FixedUnitAmount != null && FixedUnitAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monto fijo por unidad no puede ser negativo");
        }
        if ("1000".equals(TaxCod)) {
            TaxRateValue = STANDARD_IGV_RATE;
        }
        if (TaxCalculationType.equals("P") && IsInformative.equals("N")
                && (TaxRateValue == null || TaxRateValue.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Tributo porcentual requiere tasa mayor a cero");
        }
        if (TaxCalculationType.equals("F") && TaxRateValue != null && TaxRateValue.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Tributo fijo por unidad no debe tener tasa porcentual");
        }
        if (TaxCalculationType.equals("N")) {
            TaxRateValue = BigDecimal.ZERO;
            FixedUnitAmount = BigDecimal.ZERO;
        }
        return this;
    }

    @Override
    public ProductTaxConfigEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
