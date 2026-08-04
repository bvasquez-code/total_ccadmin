package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDetID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "credit_note_det")
@IdClass(CreditNoteDetID.class)
public class CreditNoteDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CreditNoteCod;

    @Id
    public int ItemNumber;

    public String ProductCod;

    public String Variant;

    public Integer NumUnit;
    public BigDecimal NumUnitPriceSale;
    public BigDecimal NumTotalPrice;
    public Integer NumUnitStockReturned;
    public BigDecimal NumPriceSubTotal = BigDecimal.ZERO;
    public BigDecimal NumTotalTax = BigDecimal.ZERO;
    public String ProductUnitName = "NIU";
    public int ProductUnitFactor = 1;
    public String IsDigital = "N";
    public String IsAppliedTax;
    public String LotNumber;
    public Date ExpirationDate;

    @Transient
    public List<CreditNoteDetTaxEntity> TaxDetailList;

    public CreditNoteDetEntity() {
    }

    public CreditNoteDetEntity validate(){
        if(this.NumUnit <= 0){
            throw new SaleBuildException("Numero de unidades debe ser mayor a cero");
        }
        if(this.NumUnitPriceSale.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Precio de ventas debe se mayor a cero");
        }
        if(this.NumTotalPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Precio total debe ser mayor a cero");
        }
        if(this.NumPriceSubTotal == null){
            this.NumPriceSubTotal = BigDecimal.ZERO;
        }
        if(this.NumTotalTax == null){
            this.NumTotalTax = BigDecimal.ZERO;
        }
        if(this.NumPriceSubTotal.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Sub total no puede ser negativo");
        }
        if(this.NumTotalTax.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Impuesto total no puede ser negativo");
        }
        return this;
    }

    @Override
    public CreditNoteDetEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
