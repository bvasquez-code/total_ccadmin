package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "tax" )
public class TaxEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TaxCod;
    public String SunatTaxCod;
    public BigDecimal TaxRateValue;
    public BigDecimal FixedUnitAmount;
    public String TaxCalculationType;
    public String IsInformative;
    public int CalculationOrder;
    public String Name;
    public String Description;

    public TaxEntity validate() {
        if (TaxCod == null || TaxCod.isBlank()) {
            throw new IllegalArgumentException("TaxCod requerido");
        }
        if (Name == null || Name.isBlank()) {
            throw new IllegalArgumentException("Nombre de tributo requerido");
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
        if (TaxRateValue == null) {
            TaxRateValue = BigDecimal.ZERO;
        }
        if (FixedUnitAmount == null) {
            FixedUnitAmount = BigDecimal.ZERO;
        }
        if (TaxRateValue.compareTo(BigDecimal.ZERO) < 0 || FixedUnitAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valores tributarios no pueden ser negativos");
        }
        if (TaxCalculationType.equals("F") && TaxRateValue.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Tributo fijo por unidad no debe tener tasa porcentual");
        }
        if (TaxCalculationType.equals("N")) {
            TaxRateValue = BigDecimal.ZERO;
            FixedUnitAmount = BigDecimal.ZERO;
        }
        return this;
    }

    @Override
    public TaxEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
