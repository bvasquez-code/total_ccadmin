package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.KardexExcepcion;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
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

    public KardexEntity(
            KardexEntity kardexLast,
            CreditNoteDetEntity creditNoteDet,
            String storeCod,
            String warehouseCod,
            int numStockMoved,
            String typeOperation
    ) {
        this.OperationCod = creditNoteDet.CreditNoteCod;
        this.ItemNumber = creditNoteDet.ItemNumber;
        this.SourceTable = "credit_note_head";
        this.TypeOperation = typeOperation;
        this.ProductCod = creditNoteDet.ProductCod;
        this.Variant = creditNoteDet.Variant;
        this.StoreCod = storeCod;
        this.WarehouseCod = warehouseCod;
        this.NumStockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
        this.NumStockMoved = numStockMoved;
        this.NumStockAfter = "R".equals(typeOperation)
                ? this.NumStockBefore - numStockMoved
                : this.NumStockBefore + numStockMoved;
        this.LotNumber = creditNoteDet.LotNumber;
        this.ExpirationDate = creditNoteDet.ExpirationDate;
        this.TypeOperationCod = 4;

        if ("R".equals(typeOperation)) {
            validateNonNegativeStock();
        }
    }

    public KardexEntity(KardexEntity kardexLast, TransferDetEntity transferDet, String storeCod, String warehouseCod, String typeOperation) {
        int stockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
        int stockMoved = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation) ? transferDet.NumUnitDispatch : transferDet.NumUnitReception;

        this.OperationCod = transferDet.TransferCod;
        this.ItemNumber = transferDet.ItemNumber;
        this.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
        this.TypeOperation = typeOperation;
        this.ProductCod = transferDet.ProductCod;
        this.Variant = transferDet.Variant;
        this.StoreCod = storeCod;
        this.WarehouseCod = warehouseCod;
        this.NumStockBefore = stockBefore;
        this.NumStockMoved = stockMoved;
        this.NumStockAfter = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation)
                ? stockBefore - stockMoved
                : stockBefore + stockMoved;
        this.LotNumber = transferDet.LotNumber;
        this.ExpirationDate = transferDet.ExpirationDate;
        this.TypeOperationCod = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation) ? 5 : 6;

        if (TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation)) {
            validateNonNegativeStock();
        }
    }

    public KardexEntity(KardexEntity kardexLast, TransferRequestDetEntity transferRequestDet, String storeCod, String warehouseCod, String typeOperation) {
        int stockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
        int stockMoved = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation) ? transferRequestDet.NumUnit : transferRequestDet.NumUnitReception;

        this.OperationCod = transferRequestDet.TransferReqCod;
        this.ItemNumber = transferRequestDet.ItemNumber;
        this.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
        this.TypeOperation = typeOperation;
        this.ProductCod = transferRequestDet.ProductCod;
        this.Variant = transferRequestDet.Variant;
        this.StoreCod = storeCod;
        this.WarehouseCod = warehouseCod;
        this.NumStockBefore = stockBefore;
        this.NumStockMoved = stockMoved;
        this.NumStockAfter = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation)
                ? stockBefore - stockMoved
                : stockBefore + stockMoved;
        this.LotNumber = transferRequestDet.LotNumber;
        this.ExpirationDate = transferRequestDet.ExpirationDate;
        this.TypeOperationCod = TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation) ? 5 : 6;

        if (TransferConstants.KARDEX_TYPE_OUT.equals(typeOperation)) {
            validateNonNegativeStock();
        }
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
