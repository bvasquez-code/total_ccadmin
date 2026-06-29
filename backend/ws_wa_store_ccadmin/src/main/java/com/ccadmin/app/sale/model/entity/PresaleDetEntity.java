package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import com.ccadmin.app.sale.model.entity.id.PresaleDetID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table( name = "presale_det" )
@IdClass(PresaleDetID.class)
public class PresaleDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PresaleCod;
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
    public String LotNumber;
    public Date ExpirationDate;

    @Transient
    public List<PresaleDetWarehouseEntity> DetailWarehouse;

    @Transient
    public ProductEntity Product;

    public PresaleDetEntity validate() throws PresaleBuildException {
        if(this.NumUnit <= 0){
            throw new PresaleBuildException("Número de productos deben ser mayor a cero");
        }
        if(this.NumUnitPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new PresaleBuildException("Precio unitario no puede ser menor a cero.");
        }
        if(this.NumUnitPriceSale.compareTo(BigDecimal.ZERO) < 0){
            throw new PresaleBuildException("Precio unitario no puede ser menor a cero.");
        }
        if(this.NumTotalPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new PresaleBuildException("Precio unitario no puede ser menor a cero.");
        }
        return this;
    }
}
