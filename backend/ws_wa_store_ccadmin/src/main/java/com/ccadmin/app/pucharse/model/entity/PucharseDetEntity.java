package com.ccadmin.app.pucharse.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.pucharse.exception.PucharseException;
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

    public PucharseDetEntity validate() throws PucharseException {
        if (this.LotNumber != null && this.LotNumber.length() > 32) {
            throw new PucharseException("El lote no puede superar 32 caracteres");
        }
        return this;
    }

    public static PucharseDetEntity buildLotDetail(PucharseDetEntity originDet, PucharseDetEntity lotDet, int itemNumber, boolean isOriginLine, String userCod) {
        PucharseDetEntity detail = isOriginLine ? originDet : new PucharseDetEntity();
        int numUnit = lotDet.NumUnitDelivered > 0 ? lotDet.NumUnitDelivered : lotDet.NumUnit;

        detail.PucharseCod = originDet.PucharseCod;
        detail.ItemNumber = itemNumber;
        detail.ProductCod = originDet.ProductCod;
        detail.Variant = originDet.Variant;
        detail.NumUnit = numUnit;
        detail.NumUnitDelivered = numUnit;
        detail.NumUnitPrice = originDet.NumUnitPrice;
        detail.NumTotalPrice = originDet.NumUnitPrice == null ? BigDecimal.ZERO : originDet.NumUnitPrice.multiply(BigDecimal.valueOf(numUnit));
        detail.IsKardexAffected = "S";
        detail.LotNumber = lotDet.LotNumber;
        detail.ExpirationDate = lotDet.ExpirationDate;
        detail.Status = "A";
        detail.addSession(userCod, !isOriginLine);

        return detail;
    }

}
