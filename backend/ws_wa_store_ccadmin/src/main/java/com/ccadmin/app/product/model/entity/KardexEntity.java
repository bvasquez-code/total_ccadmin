package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.KardexExcepcion;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Table(name = "kardex")
public class KardexEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long kardexID;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String TypeOperation;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public int NumStockBefore;
    public int NumStockMoved;
    public int NumStockAfter;
    public String LotNumber;
    public Date ExpirationDate;
    public int TypeOperationCod;

    public KardexEntity() {

    }

    public KardexEntity(KardexEntity kardexLast, PucharseDetDeliveryEntity pucharseDetDelivery, String StoreCod) {
        this.OperationCod = pucharseDetDelivery.PucharseCod;
        this.ItemNumber = pucharseDetDelivery.ItemNumber;
        this.SourceTable = "pucharse_head";
        this.TypeOperation = "S";
        this.ProductCod = pucharseDetDelivery.ProductCod;
        this.Variant = pucharseDetDelivery.Variant;
        this.StoreCod = StoreCod;
        this.WarehouseCod = pucharseDetDelivery.WarehouseCod;
        this.NumStockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
        this.NumStockMoved = pucharseDetDelivery.NumUnit;
        this.NumStockAfter = this.NumStockBefore + pucharseDetDelivery.NumUnit;
        this.LotNumber = pucharseDetDelivery.LotNumber;
        this.ExpirationDate = pucharseDetDelivery.ExpirationDate;
        this.TypeOperationCod = 2;
    }

    public KardexEntity(KardexEntity kardexLast, SaleDetWarehouseEntity saleDetWarehouse, String StoreCod) {
        this.OperationCod = saleDetWarehouse.SaleCod;
        this.ItemNumber = saleDetWarehouse.ItemNumber;
        this.SourceTable = "sale_head";
        this.TypeOperation = "R";
        this.ProductCod = saleDetWarehouse.ProductCod;
        this.Variant = saleDetWarehouse.Variant;
        this.StoreCod = StoreCod;
        this.WarehouseCod = saleDetWarehouse.WarehouseCod;
        this.NumStockBefore = kardexLast.NumStockAfter;
        this.NumStockMoved = saleDetWarehouse.NumUnit;
        this.NumStockAfter = this.NumStockBefore - saleDetWarehouse.NumUnit;
        this.LotNumber = saleDetWarehouse.LotNumber;
        this.ExpirationDate = saleDetWarehouse.ExpirationDate;
        this.TypeOperationCod = 1;
        validateNonNegativeStock();
    }

    public KardexEntity(KardexEntity kardexLast, CreditNoteDetWarehouseEntity creditNoteDetWarehouse, String StoreCod) {
        this.OperationCod = creditNoteDetWarehouse.CreditNoteCod;
        this.ItemNumber = creditNoteDetWarehouse.ItemNumber;
        this.SourceTable = "credit_note_head";
        this.TypeOperation = "S";
        this.ProductCod = creditNoteDetWarehouse.ProductCod;
        this.Variant = creditNoteDetWarehouse.Variant;
        this.StoreCod = StoreCod;
        this.WarehouseCod = creditNoteDetWarehouse.WarehouseCod;
        this.NumStockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
        this.NumStockMoved = creditNoteDetWarehouse.NumUnit;
        this.NumStockAfter = this.NumStockBefore + creditNoteDetWarehouse.NumUnit;
        this.LotNumber = creditNoteDetWarehouse.LotNumber;
        this.ExpirationDate = creditNoteDetWarehouse.ExpirationDate;
        this.TypeOperationCod = 4;
    }

    @Override
    public KardexEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

    public void validateNonNegativeStock() {
        if (this.NumStockAfter < 0) {
            throw new KardexExcepcion(
                    "Stock negativo no permitido. " +
                            "ProductCod=" + this.ProductCod +
                            ", Variant=" + this.Variant +
                            ", StoreCod=" + this.StoreCod +
                            ", WarehouseCod=" + this.WarehouseCod +
                            ", NumStockBefore=" + this.NumStockBefore +
                            ", NumStockMoved=" + this.NumStockMoved +
                            ", NumStockAfter=" + this.NumStockAfter);
        }
    }
}
