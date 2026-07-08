package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDetTaxID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "credit_note_det_tax")
@IdClass(CreditNoteDetTaxID.class)
public class CreditNoteDetTaxEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CreditNoteCod;
    @Id
    public int ItemNumber;
    @Id
    public int TaxLineNumber;
    public String TaxCod;
    public String SunatTaxCod;
    public String TaxName;
    public String TaxAffectationCod;
    public String TaxAffectationName;
    public String TaxCalculationType;
    public String IsInformative;
    public BigDecimal TaxRateValue;
    public BigDecimal FixedUnitAmount;
    public BigDecimal TaxBaseAmount = BigDecimal.ZERO;
    public BigDecimal TaxQuantity = BigDecimal.ZERO;
    public BigDecimal TaxAmount = BigDecimal.ZERO;
    public int CalculationOrder;

    public CreditNoteDetTaxEntity validate() {
        if (CreditNoteCod == null || CreditNoteCod.isBlank()) {
            throw new SaleBuildException("Codigo de nota de credito esta vacio");
        }
        if (ItemNumber <= 0 || TaxLineNumber <= 0) {
            throw new SaleBuildException("Numero de linea tributaria de nota de credito no valido");
        }
        if (TaxCod == null || TaxCod.isBlank()) {
            throw new SaleBuildException("Codigo de tributo no puede ser vacio");
        }
        if (TaxName == null || TaxName.isBlank()) {
            throw new SaleBuildException("Nombre de tributo no puede ser vacio");
        }
        if (TaxCalculationType == null || TaxCalculationType.isBlank()) {
            throw new SaleBuildException("Tipo de calculo tributario no puede ser vacio");
        }
        if (IsInformative == null || IsInformative.isBlank()) {
            IsInformative = "N";
        }
        if (TaxRateValue == null) {
            TaxRateValue = BigDecimal.ZERO;
        }
        if (FixedUnitAmount == null) {
            FixedUnitAmount = BigDecimal.ZERO;
        }
        if (TaxBaseAmount == null) {
            TaxBaseAmount = BigDecimal.ZERO;
        }
        if (TaxQuantity == null) {
            TaxQuantity = BigDecimal.ZERO;
        }
        if (TaxAmount == null) {
            TaxAmount = BigDecimal.ZERO;
        }
        if (TaxRateValue.compareTo(BigDecimal.ZERO) < 0
                || FixedUnitAmount.compareTo(BigDecimal.ZERO) < 0
                || TaxBaseAmount.compareTo(BigDecimal.ZERO) < 0
                || TaxQuantity.compareTo(BigDecimal.ZERO) < 0
                || TaxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaleBuildException("Valores tributarios de nota de credito no pueden ser negativos");
        }
        return this;
    }

    @Override
    public CreditNoteDetTaxEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
