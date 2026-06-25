package com.ccadmin.app.pucharse.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseRequestDetId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "pucharse_request_det")
@IdClass(PucharseRequestDetId.class)
public class PucharseRequestDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PucharseReqCod;
    @Id
    public String ProductCod;
    @Id
    public String Variant;
    public int NumUnit;
    public BigDecimal NumUnitPrice;
    public BigDecimal NumTotalPrice;

    @Transient
    public ProductEntity Product;

    public PucharseRequestDetEntity()
    {

    }

}
