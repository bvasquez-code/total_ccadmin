package com.ccadmin.app.pucharse.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseDetId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table( name = "pucharse_det")
@IdClass(PucharseDetId.class)
public class PucharseDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PucharseCod;
    @Id
    public int ItemNumber;
    public String ProductCod;
    public String Variant;
    public int NumUnit;
    public BigDecimal NumUnitPrice;
    public BigDecimal NumTotalPrice;
    public String IsKardexAffected;
    public int NumUnitDelivered;
    public String LotNumber;
    public Date ExpirationDate;

    @Transient
    public ProductEntity Product;

    public PucharseDetEntity()
    {
        this.IsKardexAffected = "N";
    }
    public PucharseDetEntity(PucharseRequestDetEntity pucharseRequestDet)
    {
        this.ProductCod = pucharseRequestDet.ProductCod;
        this.Variant = pucharseRequestDet.Variant;
        this.NumUnit = pucharseRequestDet.NumUnit;
        this.NumUnitPrice = pucharseRequestDet.NumUnitPrice;
        this.NumTotalPrice = pucharseRequestDet.NumTotalPrice;
    }

}
