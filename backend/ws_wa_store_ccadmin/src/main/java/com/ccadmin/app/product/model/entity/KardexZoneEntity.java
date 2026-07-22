package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.KardexZoneException;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "kardex_zone")
public class KardexZoneEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long KardexZoneID;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String MovementEvent;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public String ZoneStockMoved;
    public String TypeOperation;
    public int NumStockMoved;
    public int NumZoneStockBefore;
    public int NumZoneStockAfter;
    public String LotNumber;
    public Date ExpirationDate;

    public KardexZoneEntity() {
    }

    public static boolean isValidPresaleReservation(
            List<KardexZoneEntity> reservationList,
            SaleDetWarehouseEntity detail
    ) {
        boolean physicalWasSubtracted = reservationList.stream().anyMatch(movement ->
                matches(movement, detail, KardexZoneConstants.ZONE_PHYSICAL,
                        KardexZoneConstants.TYPE_OPERATION_SUBTRACT)
        );
        boolean reservedWasAdded = reservationList.stream().anyMatch(movement ->
                matches(movement, detail, KardexZoneConstants.ZONE_RESERVED,
                        KardexZoneConstants.TYPE_OPERATION_ADD)
        );
        return physicalWasSubtracted && reservedWasAdded;
    }

    public static List<KardexZoneEntity> buildPresaleReservation(
            PresaleHeadEntity head,
            PresaleDetWarehouseEntity detail,
            String userCod
    ) {
        return build(
                head.PresaleCod, detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION,
                detail.ProductCod, detail.Variant, head.StoreCod, detail.WarehouseCod,
                detail.LotNumber, detail.ExpirationDate, userCod,
                movement(KardexZoneConstants.ZONE_PHYSICAL, -detail.NumUnit),
                movement(KardexZoneConstants.ZONE_RESERVED, detail.NumUnit)
        );
    }

    public static List<KardexZoneEntity> buildSaleConfirmation(
            SaleHeadEntity head,
            SaleDetWarehouseEntity detail,
            String userCod
    ) {
        return build(
                head.SaleCod, detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                SaleConstants.KARDEX_ZONE_EVENT_CONFIRMATION,
                detail.ProductCod, detail.Variant, head.StoreCod, detail.WarehouseCod,
                detail.LotNumber, detail.ExpirationDate, userCod,
                movement(KardexZoneConstants.ZONE_RESERVED, -detail.NumUnit),
                movement(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit),
                movement(KardexZoneConstants.ZONE_PHYSICAL, -detail.NumUnit)
        );
    }

    public static List<KardexZoneEntity> buildSaleExpirationRelease(
            SaleHeadEntity head,
            SaleDetWarehouseEntity detail,
            String userCod
    ) {
        return build(
                head.SaleCod, detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                SaleConstants.KARDEX_ZONE_EVENT_EXPIRATION_RELEASE,
                detail.ProductCod, detail.Variant, head.StoreCod, detail.WarehouseCod,
                detail.LotNumber, detail.ExpirationDate, userCod,
                movement(KardexZoneConstants.ZONE_RESERVED, -detail.NumUnit),
                movement(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit)
        );
    }

    public static List<KardexZoneEntity> buildPurchaseReceipt(
            PucharseHeadEntity head,
            PucharseDetDeliveryEntity detail,
            String userCod
    ) {
        return build(
                head.PucharseCod, detail.ItemNumber,
                PucharseConstants.KARDEX_ZONE_SOURCE,
                PucharseConstants.KARDEX_ZONE_EVENT_RECEIPT,
                detail.ProductCod, detail.Variant, head.StoreCod, detail.WarehouseCod,
                detail.LotNumber, detail.ExpirationDate, userCod,
                movement(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit)
        );
    }

    public static List<KardexZoneEntity> buildTransferDispatch(
            String operationCod,
            String sourceTable,
            String storeCod,
            int itemNumber,
            String productCod,
            String variant,
            String warehouseCod,
            int quantity,
            String lotNumber,
            Date expirationDate,
            String userCod
    ) {
        return build(
                operationCod, itemNumber, sourceTable,
                TransferConstants.KARDEX_ZONE_EVENT_DISPATCH,
                productCod, variant, storeCod, warehouseCod,
                lotNumber, expirationDate, userCod,
                movement(KardexZoneConstants.ZONE_PHYSICAL, -quantity)
        );
    }

    public static List<KardexZoneEntity> buildTransferReceipt(
            String operationCod,
            String sourceTable,
            String storeCod,
            int itemNumber,
            String productCod,
            String variant,
            String warehouseCod,
            int quantity,
            String lotNumber,
            Date expirationDate,
            String userCod
    ) {
        return build(
                operationCod, itemNumber, sourceTable,
                TransferConstants.KARDEX_ZONE_EVENT_RECEIPT,
                productCod, variant, storeCod, warehouseCod,
                lotNumber, expirationDate, userCod,
                movement(KardexZoneConstants.ZONE_PHYSICAL, quantity)
        );
    }

    public static List<KardexZoneEntity> buildCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            String userCod
    ) {
        return buildCreditNote(
                head, detail, warehouse, userCod,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION,
                movement(KardexZoneConstants.ZONE_UNAVAILABLE, detail.NumUnit)
        );
    }

    public static List<KardexZoneEntity> buildCreditNoteAcceptedReturn(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            int returned,
            String userCod
    ) {
        return buildCreditNote(
                head, detail, warehouse, userCod,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_ACCEPTED_RETURN,
                movement(KardexZoneConstants.ZONE_UNAVAILABLE, -returned),
                movement(KardexZoneConstants.ZONE_PHYSICAL, returned)
        );
    }

    public static List<KardexZoneEntity> buildCreditNoteRejectedStockExit(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            int rejected,
            String userCod
    ) {
        return buildCreditNote(
                head, detail, warehouse, userCod,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_REJECTED_STOCK_EXIT,
                movement(KardexZoneConstants.ZONE_UNAVAILABLE, -rejected)
        );
    }

    private static List<KardexZoneEntity> buildCreditNote(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            String userCod,
            String movementEvent,
            ZoneMovement... movementList
    ) {
        return build(
                head.CreditNoteCod, detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE, movementEvent,
                detail.ProductCod, detail.Variant, head.StoreCod, warehouse.WarehouseCod,
                detail.LotNumber, detail.ExpirationDate, userCod, movementList
        );
    }

    private static List<KardexZoneEntity> build(
            String operationCod,
            int itemNumber,
            String sourceTable,
            String movementEvent,
            String productCod,
            String variant,
            String storeCod,
            String warehouseCod,
            String lotNumber,
            Date expirationDate,
            String userCod,
            ZoneMovement... movementList
    ) {
        List<KardexZoneEntity> result = new ArrayList<>();

        for (ZoneMovement movement : movementList) {
            if (!KardexZoneConstants.isSupported(movement.zone)) {
                throw new KardexZoneException("Zona de stock no soportada");
            }
            if (movement.delta == 0) {
                throw new KardexZoneException("La cantidad movida debe ser diferente de cero");
            }
            KardexZoneEntity entity = new KardexZoneEntity();
            entity.OperationCod = operationCod;
            entity.ItemNumber = itemNumber;
            entity.SourceTable = sourceTable;
            entity.MovementEvent = movementEvent;
            entity.ProductCod = productCod;
            entity.Variant = variant;
            entity.StoreCod = storeCod;
            entity.WarehouseCod = warehouseCod;
            entity.ZoneStockMoved = movement.zone;
            entity.TypeOperation = movement.delta > 0
                    ? KardexZoneConstants.TYPE_OPERATION_ADD
                    : KardexZoneConstants.TYPE_OPERATION_SUBTRACT;
            entity.NumStockMoved = Math.abs(movement.delta);
            entity.LotNumber = lotNumber;
            entity.ExpirationDate = expirationDate;
            entity.addSession(userCod);
            result.add(entity);
        }
        return List.copyOf(result);
    }

    public void applyLastMovement(KardexZoneEntity lastMovement) {
        this.NumZoneStockBefore = lastMovement == null ? 0 : lastMovement.NumZoneStockAfter;
        this.NumZoneStockAfter = this.NumZoneStockBefore + this.signedQuantity();
        this.validateNonNegativeStock();
    }

    public void applyCurrentStock(int currentStock) {
        this.NumZoneStockBefore = currentStock;
        this.NumZoneStockAfter = this.NumZoneStockBefore + this.signedQuantity();
        this.validateNonNegativeStock();
    }

    public int signedQuantity() {
        if (KardexZoneConstants.TYPE_OPERATION_ADD.equals(this.TypeOperation)) {
            return this.NumStockMoved;
        }
        if (KardexZoneConstants.TYPE_OPERATION_SUBTRACT.equals(this.TypeOperation)) {
            return -this.NumStockMoved;
        }
        throw new KardexZoneException("Tipo de operacion de zona no soportado");
    }

    public void validateNonNegativeStock() {
        if (this.NumZoneStockAfter < 0) {
            throw new KardexZoneException(
                    "Stock insuficiente en zona " + this.ZoneStockMoved
                            + " para producto " + this.ProductCod
            );
        }
    }

    private static ZoneMovement movement(String zone, int delta) {
        return new ZoneMovement(zone, delta);
    }

    private static boolean matches(
            KardexZoneEntity movement,
            SaleDetWarehouseEntity detail,
            String zone,
            String typeOperation
    ) {
        return zone.equals(movement.ZoneStockMoved)
                && typeOperation.equals(movement.TypeOperation)
                && detail.NumUnit == movement.NumStockMoved
                && detail.ProductCod.equals(movement.ProductCod)
                && detail.Variant.equals(movement.Variant)
                && detail.WarehouseCod.equals(movement.WarehouseCod);
    }

    private record ZoneMovement(String zone, int delta) {
    }
}
