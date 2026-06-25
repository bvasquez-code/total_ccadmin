package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.entity.id.SaleAppliedTaxID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table ( name = "sale_applied_tax" )
@IdClass( SaleAppliedTaxID.class )
public class SaleAppliedTaxEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TaxCod;
    @Id
    public String SaleCod;
    public BigDecimal TaxRateValue;

    public SaleAppliedTaxEntity() {
    }

    public SaleAppliedTaxEntity(String taxCod, String saleCod, BigDecimal taxRateValue) {
        TaxCod = taxCod;
        SaleCod = saleCod;
        TaxRateValue = taxRateValue;
    }

    public SaleAppliedTaxEntity build(String taxCod, String saleCod, BigDecimal taxRateValue) {
        TaxCod = taxCod;
        SaleCod = saleCod;
        TaxRateValue = taxRateValue;
        return this;
    }

    public SaleAppliedTaxEntity validate() {
        if(this.TaxCod == null || this.TaxCod.isEmpty()){
            throw new SaleBuildException("Código de impuesto no puede ser vacío");
        }
        if(this.TaxRateValue.compareTo(BigDecimal.ZERO)<0){
            throw new SaleBuildException("valor de impuesto no puede ser menor que cero");
        }
        return this;
    }

    @Override
    public SaleAppliedTaxEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
