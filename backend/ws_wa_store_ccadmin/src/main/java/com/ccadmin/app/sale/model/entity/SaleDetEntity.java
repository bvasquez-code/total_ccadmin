package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.entity.id.SaleDetID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table( name = "sale_det")
@IdClass( SaleDetID.class )
public class SaleDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SaleCod;
    @Id
    public int ItemNumber;
    public String ProductCod;
    public String Variant;
    public int NumUnit;
    public BigDecimal NumUnitPrice;
    public BigDecimal NumDiscount;
    public BigDecimal NumUnitPriceSale;
    public BigDecimal NumTotalPrice;
    public String ProductUnitName = "NIU";
    public int ProductUnitFactor = 1;
    public String IsAppliedTax;
    public String LotNumber;
    public Date ExpirationDate;

    @Transient
    public List<SaleDetWarehouseEntity> DetailWarehouse;
    @Transient
    public ProductEntity Product;

    public SaleDetEntity()
    {

    }

    public SaleDetEntity(PresaleDetEntity presaleDet,String SaleCod)
    {
        this.SaleCod = SaleCod;
        this.ItemNumber = presaleDet.ItemNumber;
        this.ProductCod = presaleDet.ProductCod;
        this.Variant = presaleDet.Variant;
        this.NumUnit = presaleDet.NumUnit;
        this.NumUnitPrice = presaleDet.NumUnitPrice;
        this.NumDiscount = presaleDet.NumDiscount;
        this.NumUnitPriceSale = presaleDet.NumUnitPriceSale;
        this.NumTotalPrice = presaleDet.NumTotalPrice;
        this.ProductUnitName = presaleDet.ProductUnitName;
        this.ProductUnitFactor = presaleDet.ProductUnitFactor;
        this.IsAppliedTax = "S";
        this.LotNumber = presaleDet.LotNumber;
        this.ExpirationDate = presaleDet.ExpirationDate;
    }

    public SaleDetEntity build(PresaleDetEntity presaleDet,String SaleCod){
        this.ProductCod = presaleDet.ProductCod;
        this.ItemNumber = presaleDet.ItemNumber;
        this.Variant = presaleDet.Variant;
        this.NumUnit = presaleDet.NumUnit;
        this.NumUnitPrice = presaleDet.NumUnitPrice;
        this.NumDiscount = presaleDet.NumDiscount;
        this.NumUnitPriceSale = presaleDet.NumUnitPriceSale;
        this.NumTotalPrice = presaleDet.NumTotalPrice;
        this.ProductUnitName = presaleDet.ProductUnitName;
        this.ProductUnitFactor = presaleDet.ProductUnitFactor;
        this.IsAppliedTax = "S";
        this.SaleCod = SaleCod;
        this.LotNumber = presaleDet.LotNumber;
        this.ExpirationDate = presaleDet.ExpirationDate;
        return this;
    }

    public SaleDetEntity validate() throws SaleBuildException {
        if(this.SaleCod==null || this.SaleCod.isEmpty()){
            throw new SaleBuildException("Código de venta esta vacío");
        }
        if(this.NumUnit <= 0){
            throw new SaleBuildException("Número de unidades del productos debe ser mayor a cero");
        }
        if(this.NumUnitPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Sub total no puede ser negativo");
        }
        if(this.NumDiscount.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Descuento no puede ser negativo");
        }
        if(this.NumTotalPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Precio total no puede ser negativo");
        }
        if(this.NumUnitPriceSale.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Precio unitario no puede ser negativo");
        }
        return this;
    }

    @Override
    public SaleDetEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
