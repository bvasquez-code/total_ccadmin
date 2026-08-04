package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.KardexExcepcion;
import com.ccadmin.app.product.exception.KardexZoneException;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.shared.ProductFindCreateShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

@Service
public class KardexCreateService {

    private static final Comparator<StockKey> STOCK_KEY_COMPARATOR = Comparator
            .comparing(StockKey::productCod)
            .thenComparing(StockKey::variant)
            .thenComparing(StockKey::storeCod)
            .thenComparing(StockKey::warehouseCod);

    @Autowired
    private KardexRepository kardexRepository;
    @Autowired
    private KardexZoneRepository kardexZoneRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;

    public List<KardexZoneEntity> buildPresaleReservation(
            PresaleHeadEntity presaleHead,
            List<PresaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        List<KardexZoneEntity> result = new ArrayList<>();
        for (PresaleDetWarehouseEntity detail : detailList) {
            result.addAll(KardexZoneEntity.buildPresaleReservation(
                    presaleHead, detail, userCod
            ));
        }
        return List.copyOf(result);
    }

    public List<KardexEntity> buildSaleConfirmation(
            SaleHeadEntity saleHead,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        return detailList.stream().map(detail -> KardexEntity.build(
                saleHead.SaleCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT,
                detail.ProductCod,
                detail.Variant,
                saleHead.StoreCod,
                detail.WarehouseCod,
                detail.NumUnit,
                detail.LotNumber,
                detail.ExpirationDate,
                1,
                userCod
        )).toList();
    }

    public List<KardexZoneEntity> buildZoneSaleConfirmation(
            SaleHeadEntity saleHead,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        if (saleHead.PresaleCod == null || saleHead.PresaleCod.isBlank()) {
            throw new SaleException("La venta no tiene una preventa reservada asociada");
        }
        this.validatePresaleReservations(saleHead, detailList);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (SaleDetWarehouseEntity detail : detailList) {
            result.addAll(KardexZoneEntity.buildSaleConfirmation(
                    saleHead, detail, userCod
            ));
        }
        return List.copyOf(result);
    }

    public List<KardexZoneEntity> buildSaleExpirationRelease(
            SaleHeadEntity saleHead,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        if (saleHead.PresaleCod == null || saleHead.PresaleCod.isBlank()) {
            throw new SaleException("La venta no tiene una preventa reservada asociada");
        }
        this.validatePresaleReservations(saleHead, detailList);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (SaleDetWarehouseEntity detail : detailList) {
            result.addAll(KardexZoneEntity.buildSaleExpirationRelease(
                    saleHead, detail, userCod
            ));
        }
        return List.copyOf(result);
    }

    public List<KardexEntity> buildPurchaseReceipt(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList,
            String userCod
    ) throws PucharseException {
        this.validatePurchaseReceipt(purchaseHead, deliveryList);
        return deliveryList.stream().map(detail -> KardexEntity.build(
                purchaseHead.PucharseCod,
                detail.ItemNumber,
                PucharseConstants.KARDEX_ZONE_SOURCE,
                KardexZoneConstants.TYPE_OPERATION_ADD,
                detail.ProductCod,
                detail.Variant,
                purchaseHead.StoreCod,
                detail.WarehouseCod,
                detail.NumUnit,
                detail.LotNumber,
                detail.ExpirationDate,
                2,
                userCod
        )).toList();
    }

    public List<KardexZoneEntity> buildZonePurchaseReceipt(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList,
            String userCod
    ) throws PucharseException {
        this.validatePurchaseReceipt(purchaseHead, deliveryList);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (PucharseDetDeliveryEntity detail : deliveryList) {
            result.addAll(KardexZoneEntity.buildPurchaseReceipt(
                    purchaseHead, detail, userCod
            ));
        }
        return List.copyOf(result);
    }

    private void validatePurchaseReceipt(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList
    ) throws PucharseException {
        if (purchaseHead == null || purchaseHead.PucharseCod == null
                || purchaseHead.PucharseCod.isBlank()) {
            throw new PucharseException("La compra es obligatoria para recibir stock");
        }
        if (deliveryList == null || deliveryList.isEmpty()) {
            throw new PucharseException("La compra no tiene entregas para recibir");
        }
        for (PucharseDetDeliveryEntity delivery : deliveryList) {
            if (delivery == null || delivery.NumUnit <= 0) {
                throw new PucharseException("La cantidad recibida debe ser mayor que cero");
            }
            if (!purchaseHead.PucharseCod.equals(delivery.PucharseCod)) {
                throw new PucharseException(
                        "La entrega no corresponde a la compra " + purchaseHead.PucharseCod
                );
            }
        }
    }

    public List<KardexEntity> buildTransferDispatch(
            String operationCod,
            String storeCod,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferKardex(
                operationCod, TransferConstants.KARDEX_SOURCE_TABLE, storeCod,
                detailList.stream().filter(item -> item.NumUnitDispatch > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodOrigin, item.NumUnitDispatch,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT, 5, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferDispatch(
            String operationCod,
            String storeCod,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferZone(
                operationCod, TransferConstants.KARDEX_SOURCE_TABLE, storeCod,
                detailList.stream().filter(item -> item.NumUnitDispatch > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodOrigin, item.NumUnitDispatch,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                true, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestDispatch(
            String operationCod,
            String storeCod,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferKardex(
                operationCod, TransferConstants.KARDEX_ZONE_SOURCE_REQUEST, storeCod,
                detailList.stream().filter(item -> item.NumUnit > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodOrigin, item.NumUnit,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT, 5, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestDispatch(
            String operationCod,
            String storeCod,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferZone(
                operationCod, TransferConstants.KARDEX_ZONE_SOURCE_REQUEST, storeCod,
                detailList.stream().filter(item -> item.NumUnit > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodOrigin, item.NumUnit,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                true, userCod
        );
    }

    public List<KardexEntity> buildTransferReceipt(
            String operationCod,
            String storeCod,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferKardex(
                operationCod, TransferConstants.KARDEX_SOURCE_TABLE, storeCod,
                detailList.stream().filter(item -> item.NumUnitReception > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodDest, item.NumUnitReception,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                KardexZoneConstants.TYPE_OPERATION_ADD, 6, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferReceipt(
            String operationCod,
            String storeCod,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferZone(
                operationCod, TransferConstants.KARDEX_SOURCE_TABLE, storeCod,
                detailList.stream().filter(item -> item.NumUnitReception > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodDest, item.NumUnitReception,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                false, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestReceipt(
            String operationCod,
            String storeCod,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferKardex(
                operationCod, TransferConstants.KARDEX_ZONE_SOURCE_REQUEST, storeCod,
                detailList.stream().filter(item -> item.NumUnitReception > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodDest, item.NumUnitReception,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                KardexZoneConstants.TYPE_OPERATION_ADD, 6, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestReceipt(
            String operationCod,
            String storeCod,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        return this.buildTransferZone(
                operationCod, TransferConstants.KARDEX_ZONE_SOURCE_REQUEST, storeCod,
                detailList.stream().filter(item -> item.NumUnitReception > 0)
                        .map(item -> new TransferMovementLine(
                                item.ItemNumber, item.ProductCod, item.Variant,
                                item.WarehouseCodDest, item.NumUnitReception,
                                item.LotNumber, item.ExpirationDate
                        )).toList(),
                false, userCod
        );
    }

    private List<KardexEntity> buildTransferKardex(
            String operationCod,
            String sourceTable,
            String storeCod,
            List<TransferMovementLine> lineList,
            String typeOperation,
            int typeOperationCod,
            String userCod
    ) throws TransferException {
        this.validateTransferMovement(operationCod, lineList);
        return lineList.stream().map(line -> KardexEntity.build(
                operationCod, line.itemNumber, sourceTable, typeOperation,
                line.productCod, line.variant, storeCod, line.warehouseCod,
                line.quantity, line.lotNumber, line.expirationDate,
                typeOperationCod, userCod
        )).toList();
    }

    private List<KardexZoneEntity> buildTransferZone(
            String operationCod,
            String sourceTable,
            String storeCod,
            List<TransferMovementLine> lineList,
            boolean dispatch,
            String userCod
    ) throws TransferException {
        this.validateTransferMovement(operationCod, lineList);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (TransferMovementLine line : lineList) {
            if (dispatch) {
                result.addAll(KardexZoneEntity.buildTransferDispatch(
                        operationCod, sourceTable, storeCod, line.itemNumber,
                        line.productCod, line.variant, line.warehouseCod, line.quantity,
                        line.lotNumber, line.expirationDate, userCod
                ));
            } else {
                result.addAll(KardexZoneEntity.buildTransferReceipt(
                        operationCod, sourceTable, storeCod, line.itemNumber,
                        line.productCod, line.variant, line.warehouseCod, line.quantity,
                        line.lotNumber, line.expirationDate, userCod
                ));
            }
        }
        return List.copyOf(result);
    }

    private void validateTransferMovement(
            String operationCod,
            List<TransferMovementLine> lineList
    ) throws TransferException {
        if (operationCod == null || operationCod.isBlank()) {
            throw new TransferException("El codigo de transferencia es obligatorio");
        }
        if (lineList == null || lineList.isEmpty()) {
            throw new TransferException("La transferencia no tiene unidades para mover");
        }
        if (lineList.stream().anyMatch(item -> item.quantity <= 0
                || item.warehouseCod == null || item.warehouseCod.isBlank())) {
            throw new TransferException("La cantidad y el almacen de transferencia son obligatorios");
        }
    }

    public List<KardexEntity> buildCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validateCreditNoteStock(head, detailList, warehouse);
        return detailList.stream().map(detail -> this.buildCreditNoteKardex(
                head, detail, warehouse, detail.NumUnit,
                KardexZoneConstants.TYPE_OPERATION_ADD, userCod
        )).toList();
    }

    public List<KardexZoneEntity> buildZoneCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validateCreditNoteStock(head, detailList, warehouse);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (CreditNoteDetEntity detail : detailList) {
            result.addAll(KardexZoneEntity.buildCreditNoteConfirmation(
                    head, detail, warehouse, userCod
            ));
        }
        return List.copyOf(result);
    }

    public List<KardexEntity> buildCreditNoteRejectedExit(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validateCreditNoteStock(head, detailList, warehouse);
        List<KardexEntity> result = new ArrayList<>();
        for (CreditNoteDetEntity detail : detailList) {
            int rejected = this.rejectedQuantity(detail);
            if (rejected > 0) {
                this.validateUnavailableStockOrigin(head, detail, warehouse);
                result.add(this.buildCreditNoteKardex(
                        head, detail, warehouse, rejected,
                        KardexZoneConstants.TYPE_OPERATION_SUBTRACT, userCod
                ));
            }
        }
        return List.copyOf(result);
    }

    public List<KardexZoneEntity> buildZoneCreditNoteReturn(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validateCreditNoteStock(head, detailList, warehouse);
        List<KardexZoneEntity> result = new ArrayList<>();
        for (CreditNoteDetEntity detail : detailList) {
            int returned = this.returnedQuantity(detail);
            int rejected = detail.NumUnit - returned;
            if (returned > 0) {
                this.validateUnavailableStockOrigin(head, detail, warehouse);
                result.addAll(KardexZoneEntity.buildCreditNoteAcceptedReturn(
                        head, detail, warehouse, returned, userCod
                ));
            }
            if (rejected > 0) {
                this.validateUnavailableStockOrigin(head, detail, warehouse);
                result.addAll(KardexZoneEntity.buildCreditNoteRejectedStockExit(
                        head, detail, warehouse, rejected, userCod
                ));
            }
        }
        return List.copyOf(result);
    }

    private KardexEntity buildCreditNoteKardex(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            int quantity,
            String typeOperation,
            String userCod
    ) {
        return KardexEntity.build(
                head.CreditNoteCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                typeOperation,
                detail.ProductCod,
                detail.Variant,
                head.StoreCod,
                warehouse.WarehouseCod,
                quantity,
                detail.LotNumber,
                detail.ExpirationDate,
                4,
                userCod
        );
    }

    private int returnedQuantity(CreditNoteDetEntity detail) throws SaleException {
        int returned = detail.NumUnitStockReturned == null ? 0 : detail.NumUnitStockReturned;
        if (returned < 0 || returned > detail.NumUnit) {
            throw new SaleException("Cantidad de retorno invalida para el producto " + detail.ProductCod);
        }
        return returned;
    }

    private int rejectedQuantity(CreditNoteDetEntity detail) throws SaleException {
        return detail.NumUnit - this.returnedQuantity(detail);
    }

    private void validateUnavailableStockOrigin(
            CreditNoteHeadEntity head,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse
    ) throws SaleException {
        List<KardexZoneEntity> confirmationList = this.kardexZoneRepository.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                head.CreditNoteCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        );
        boolean unavailableWasAdded = confirmationList.stream().anyMatch(movement ->
                KardexZoneConstants.ZONE_UNAVAILABLE.equals(movement.ZoneStockMoved)
                        && KardexZoneConstants.TYPE_OPERATION_ADD.equals(movement.TypeOperation)
                        && detail.NumUnit == movement.NumStockMoved
                        && detail.ProductCod.equals(movement.ProductCod)
                        && detail.Variant.equals(movement.Variant)
                        && warehouse.WarehouseCod.equals(movement.WarehouseCod)
        );
        boolean historicalBaselineExists = !unavailableWasAdded
                && head.CreationDate != null
                && this.kardexZoneRepository.countLegacyUnavailableBaseline(
                        detail.ProductCod, detail.Variant, head.StoreCod,
                        warehouse.WarehouseCod, detail.NumUnit, head.CreationDate
                ) > 0;
        if (!unavailableWasAdded && !historicalBaselineExists) {
            throw new SaleException(
                    "No existe stock no disponible confirmado para el item " + detail.ItemNumber
                            + " de la nota de credito " + head.CreditNoteCod
            );
        }
    }

    private void validateCreditNoteStock(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse
    ) throws SaleException {
        if (head == null || head.CreditNoteCod == null || head.CreditNoteCod.isBlank()) {
            throw new SaleException("La nota de credito es obligatoria para procesar stock");
        }
        if (warehouse == null || warehouse.WarehouseCod == null || warehouse.WarehouseCod.isBlank()) {
            throw new SaleException("El almacen de la nota de credito es obligatorio");
        }
        if (detailList == null || detailList.isEmpty()) {
            throw new SaleException("La nota de credito no tiene detalle para procesar stock");
        }
        for (CreditNoteDetEntity detail : detailList) {
            if (detail == null || detail.NumUnit == null || detail.NumUnit <= 0) {
                throw new SaleException("La cantidad de la nota de credito debe ser mayor que cero");
            }
            if (!head.CreditNoteCod.equals(detail.CreditNoteCod)) {
                throw new SaleException("El detalle no corresponde a la nota de credito");
            }
        }
    }

    private void validatePresaleReservations(
            SaleHeadEntity saleHead,
            List<SaleDetWarehouseEntity> detailList
    ) throws SaleException {
        Map<StockKey, Integer> pickedQuantityByStock = new LinkedHashMap<>();
        for (SaleDetWarehouseEntity detail : detailList) {
            if (detail.NumUnit <= 0) {
                throw new SaleException("La cantidad de venta debe ser mayor que cero");
            }
            StockKey key = new StockKey(
                    detail.ProductCod, detail.Variant, saleHead.StoreCod, detail.WarehouseCod
            );
            this.mergeQuantity(pickedQuantityByStock, key, detail.NumUnit, "pickeo de venta");
        }

        List<KardexZoneEntity> reservationList = this.kardexZoneRepository.findByOperationEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                saleHead.PresaleCod,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        );
        Map<StockKey, Integer> physicalReservationByStock = new LinkedHashMap<>();
        Map<StockKey, Integer> reservedReservationByStock = new LinkedHashMap<>();
        for (KardexZoneEntity reservation : reservationList) {
            StockKey key = StockKey.from(reservation);
            if (KardexZoneConstants.ZONE_PHYSICAL.equals(reservation.ZoneStockMoved)
                    && KardexZoneConstants.TYPE_OPERATION_SUBTRACT.equals(reservation.TypeOperation)) {
                this.mergeQuantity(
                        physicalReservationByStock, key, reservation.NumStockMoved, "reserva fisica"
                );
            } else if (KardexZoneConstants.ZONE_RESERVED.equals(reservation.ZoneStockMoved)
                    && KardexZoneConstants.TYPE_OPERATION_ADD.equals(reservation.TypeOperation)) {
                this.mergeQuantity(
                        reservedReservationByStock, key, reservation.NumStockMoved, "reserva comprometida"
                );
            } else {
                throw new SaleException("La preventa tiene un movimiento de reserva no valido");
            }
        }

        if (!pickedQuantityByStock.equals(physicalReservationByStock)
                || !pickedQuantityByStock.equals(reservedReservationByStock)) {
            throw new SaleException(
                    "La cantidad pickeada de la venta " + saleHead.SaleCod
                            + " no coincide con la reserva de la preventa"
            );
        }
    }

    private <T> void mergeQuantity(
            Map<T, Integer> quantityByKey,
            T key,
            int quantity,
            String operationName
    ) throws SaleException {
        try {
            quantityByKey.merge(key, quantity, Math::addExact);
        } catch (ArithmeticException ex) {
            throw new SaleException("La cantidad de " + operationName + " excede el limite permitido");
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> saveAll(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList
    ) {
        List<KardexEntity> totalMovementList = mutableList(kardexList);
        List<KardexZoneEntity> zoneMovementList = mutableList(kardexZoneList);
        Map<ProductStoreKey, Boolean> digitalProductMap = new LinkedHashMap<>();
        totalMovementList.removeIf(item -> this.isDigital(
                item.ProductCod, item.StoreCod, digitalProductMap
        ));
        zoneMovementList.removeIf(item -> this.isDigital(
                item.ProductCod, item.StoreCod, digitalProductMap
        ));
        if (totalMovementList.isEmpty() && zoneMovementList.isEmpty()) {
            return List.of();
        }

        Map<StockKey, ProductInfoEntity> productStockMap = new LinkedHashMap<>();
        Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap = new LinkedHashMap<>();
        this.lockStock(totalMovementList, zoneMovementList, productStockMap, warehouseStockMap);

        zoneMovementList = this.filterAppliedEvents(zoneMovementList);
        totalMovementList = this.filterTotalMovements(totalMovementList, kardexZoneList, zoneMovementList);
        if (totalMovementList.isEmpty() && zoneMovementList.isEmpty()) {
            return List.of();
        }

        this.validateMovementTotals(totalMovementList, zoneMovementList);
        this.calculateTotalBalances(totalMovementList, warehouseStockMap);
        this.calculateZoneBalances(zoneMovementList, productStockMap, warehouseStockMap);
        this.validateFinalBalances(totalMovementList, zoneMovementList, warehouseStockMap);

        List<KardexEntity> savedKardexList = new ArrayList<>();
        for (KardexEntity movement : totalMovementList) {
            savedKardexList.add(this.kardexRepository.save(movement));
        }
        for (KardexZoneEntity movement : zoneMovementList) {
            this.kardexZoneRepository.save(movement);
        }

        this.saveStock(productStockMap, warehouseStockMap, zoneMovementList);
        return List.copyOf(savedKardexList);
    }

    private void lockStock(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList,
            Map<StockKey, ProductInfoEntity> productStockMap,
            Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap
    ) {
        Set<StockKey> stockKeySet = new LinkedHashSet<>();
        kardexList.forEach(item -> stockKeySet.add(StockKey.from(item)));
        kardexZoneList.forEach(item -> stockKeySet.add(StockKey.from(item)));

        stockKeySet.stream().sorted(STOCK_KEY_COMPARATOR).forEach(key -> {
            ProductInfoEntity productStock = this.productInfoRepository.findByIdForUpdate(
                    key.productCod, key.variant, key.storeCod
            ).orElseThrow(() -> new KardexZoneException(
                    "No existe stock del producto " + key.productCod + " en la tienda " + key.storeCod
            ));
            ProductInfoWarehouseEntity warehouseStock =
                    this.productInfoWarehouseRepository.findByIdForUpdate(
                            key.productCod, key.variant, key.warehouseCod
                    ).orElseThrow(() -> new KardexZoneException(
                            "No existe stock del producto " + key.productCod
                                    + " en el almacen " + key.warehouseCod
                    ));
            productStockMap.put(key, productStock);
            warehouseStockMap.put(key, warehouseStock);
        });
    }

    private List<KardexZoneEntity> filterAppliedEvents(List<KardexZoneEntity> movementList) {
        Map<EventKey, List<KardexZoneEntity>> eventMap = new LinkedHashMap<>();
        movementList.forEach(movement -> eventMap.computeIfAbsent(
                EventKey.from(movement), ignored -> new ArrayList<>()
        ).add(movement));

        List<KardexZoneEntity> result = new ArrayList<>();
        eventMap.forEach((event, eventMovements) -> {
            if (this.kardexZoneRepository.countByEvent(
                    event.sourceTable, event.operationCod, event.itemNumber, event.movementEvent
            ) == 0) {
                result.addAll(eventMovements);
            }
        });
        return result;
    }

    private List<KardexEntity> filterTotalMovements(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> requestedZoneList,
            List<KardexZoneEntity> pendingZoneList
    ) {
        if (requestedZoneList == null || requestedZoneList.isEmpty()) {
            return kardexList;
        }
        Set<DocumentItemKey> pendingItemSet = new LinkedHashSet<>();
        pendingZoneList.forEach(item -> pendingItemSet.add(DocumentItemKey.from(item)));
        return kardexList.stream()
                .filter(item -> pendingItemSet.contains(DocumentItemKey.from(item)))
                .toList();
    }

    private void validateMovementTotals(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList
    ) {
        Map<StockKey, Integer> totalDeltaMap = new LinkedHashMap<>();
        Map<StockKey, Integer> zoneDeltaMap = new LinkedHashMap<>();
        kardexList.forEach(item -> totalDeltaMap.merge(
                StockKey.from(item), item.signedQuantity(), Integer::sum
        ));
        kardexZoneList.forEach(item -> zoneDeltaMap.merge(
                StockKey.from(item), item.signedQuantity(), Integer::sum
        ));

        Set<StockKey> stockKeySet = new LinkedHashSet<>();
        stockKeySet.addAll(totalDeltaMap.keySet());
        stockKeySet.addAll(zoneDeltaMap.keySet());
        for (StockKey key : stockKeySet) {
            int totalDelta = totalDeltaMap.getOrDefault(key, 0);
            int zoneDelta = zoneDeltaMap.getOrDefault(key, 0);
            if (totalDelta != zoneDelta) {
                throw new KardexExcepcion(
                        "Los movimientos de Kardex y Kardex Zona no coinciden para el producto "
                                + key.productCod + " en el almacen " + key.warehouseCod
                );
            }
        }
    }

    private void calculateTotalBalances(
            List<KardexEntity> movementList,
            Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap
    ) {
        Map<StockKey, Integer> currentStockMap = new LinkedHashMap<>();
        List<KardexEntity> orderedList = movementList.stream()
                .sorted(Comparator.comparing(StockKey::from, STOCK_KEY_COMPARATOR))
                .toList();

        for (KardexEntity movement : orderedList) {
            if (movement.NumStockMoved <= 0) {
                throw new KardexExcepcion("La cantidad movida de Kardex debe ser mayor que cero");
            }
            StockKey key = StockKey.from(movement);
            int currentStock = currentStockMap.getOrDefault(
                    key,
                    warehouseStockMap.get(key).NumTotalStock
            );
            movement.applyCurrentStock(currentStock);
            currentStockMap.put(key, movement.NumStockAfter);
        }
    }

    private void calculateZoneBalances(
            List<KardexZoneEntity> movementList,
            Map<StockKey, ProductInfoEntity> productStockMap,
            Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap
    ) {
        Map<ZoneStockKey, Integer> currentStockMap = new LinkedHashMap<>();
        List<KardexZoneEntity> orderedList = movementList.stream()
                .sorted(Comparator.comparing(StockKey::from, STOCK_KEY_COMPARATOR))
                .toList();

        for (KardexZoneEntity movement : orderedList) {
            this.validateZoneMovement(movement);
            StockKey stockKey = StockKey.from(movement);
            ZoneStockKey zoneKey = ZoneStockKey.from(movement);
            ProductInfoWarehouseEntity warehouseStock = warehouseStockMap.get(stockKey);
            ProductInfoEntity productStock = productStockMap.get(stockKey);

            int currentStock = currentStockMap.getOrDefault(
                    zoneKey,
                    this.zoneStock(warehouseStock, zoneKey.zone)
            );
            movement.applyCurrentStock(currentStock);
            StockDelta delta = StockDelta.from(movement.ZoneStockMoved, movement.signedQuantity());
            this.apply(warehouseStock, delta);
            this.apply(productStock, delta);
            currentStockMap.put(zoneKey, movement.NumZoneStockAfter);
        }
    }

    private void validateFinalBalances(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList,
            Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap
    ) {
        Map<StockKey, KardexEntity> finalKardexMap = new LinkedHashMap<>();
        kardexList.forEach(item -> finalKardexMap.put(StockKey.from(item), item));
        Set<StockKey> affectedKeySet = new LinkedHashSet<>();
        kardexZoneList.forEach(item -> affectedKeySet.add(StockKey.from(item)));

        for (StockKey key : affectedKeySet) {
            KardexEntity finalKardex = finalKardexMap.get(key);
            if (finalKardex != null
                    && finalKardex.NumStockAfter != warehouseStockMap.get(key).NumTotalStock) {
                throw new KardexExcepcion(
                        "El saldo final de Kardex no coincide con Kardex Zona para el producto "
                                + key.productCod + " en el almacen " + key.warehouseCod
                );
            }
        }
    }

    private void saveStock(
            Map<StockKey, ProductInfoEntity> productStockMap,
            Map<StockKey, ProductInfoWarehouseEntity> warehouseStockMap,
            List<KardexZoneEntity> movementList
    ) {
        Set<StockKey> affectedStockSet = new LinkedHashSet<>();
        movementList.forEach(item -> affectedStockSet.add(StockKey.from(item)));
        Set<ProductKey> savedProductSet = new LinkedHashSet<>();
        productStockMap.forEach((key, stock) -> {
            ProductKey productKey = new ProductKey(key.productCod, key.variant, key.storeCod);
            if (affectedStockSet.contains(key) && savedProductSet.add(productKey)) {
                stock.addSession(this.userForProduct(movementList, productKey), false);
                this.productInfoRepository.save(stock);
                this.productFindCreateShared.save(
                        key.productCod, key.storeCod,
                        this.userForProduct(movementList, productKey)
                );
            }
        });
        warehouseStockMap.forEach((key, stock) -> {
            if (affectedStockSet.contains(key)) {
                stock.addSession(this.userForWarehouse(movementList, key), false);
                this.productInfoWarehouseRepository.save(stock);
            }
        });
    }

    private void validateZoneMovement(KardexZoneEntity movement) {
        if (movement == null || !KardexZoneConstants.isSupported(movement.ZoneStockMoved)) {
            throw new KardexZoneException("Zona de stock no soportada");
        }
        if (movement.NumStockMoved <= 0) {
            throw new KardexZoneException("La cantidad movida de zona debe ser mayor que cero");
        }
    }

    private int zoneStock(ProductInfoWarehouseEntity stock, String zone) {
        return switch (zone) {
            case KardexZoneConstants.ZONE_PHYSICAL -> stock.NumPhysicalStock;
            case KardexZoneConstants.ZONE_UNAVAILABLE -> stock.NumUnavailableStock;
            case KardexZoneConstants.ZONE_RESERVED -> stock.NumReservedStock;
            default -> throw new KardexZoneException("Zona de stock no soportada");
        };
    }

    private void apply(ProductInfoWarehouseEntity stock, StockDelta delta) {
        try {
            stock.applyStockAdjustment(delta.physical, delta.unavailable, delta.reserved, delta.total());
        } catch (IllegalStateException exception) {
            throw new KardexZoneException(exception.getMessage());
        }
    }

    private void apply(ProductInfoEntity stock, StockDelta delta) {
        try {
            stock.applyStockAdjustment(delta.physical, delta.unavailable, delta.reserved, delta.total());
        } catch (IllegalStateException exception) {
            throw new KardexZoneException(exception.getMessage());
        }
    }

    private String userForProduct(List<KardexZoneEntity> movementList, ProductKey key) {
        return movementList.stream()
                .filter(item -> key.productCod.equals(item.ProductCod)
                        && key.variant.equals(item.Variant)
                        && key.storeCod.equals(item.StoreCod))
                .map(item -> item.CreationUser)
                .filter(user -> user != null && !user.isBlank())
                .findFirst()
                .orElseThrow(() -> new KardexZoneException("CreationUser es obligatorio"));
    }

    private String userForWarehouse(List<KardexZoneEntity> movementList, StockKey key) {
        return movementList.stream()
                .filter(item -> key.productCod.equals(item.ProductCod)
                        && key.variant.equals(item.Variant)
                        && key.storeCod.equals(item.StoreCod)
                        && key.warehouseCod.equals(item.WarehouseCod))
                .map(item -> item.CreationUser)
                .filter(user -> user != null && !user.isBlank())
                .findFirst()
                .orElseThrow(() -> new KardexZoneException("CreationUser es obligatorio"));
    }

    private static <T> List<T> mutableList(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private boolean isDigital(
            String productCod,
            String storeCod,
            Map<ProductStoreKey, Boolean> digitalProductMap
    ) {
        ProductStoreKey key = new ProductStoreKey(productCod, storeCod);
        return digitalProductMap.computeIfAbsent(
                key,
                ignored -> this.productOperationConfigShared.isDigital(productCod, storeCod)
        );
    }

    private record EventKey(String sourceTable, String operationCod, int itemNumber, String movementEvent) {
        static EventKey from(KardexZoneEntity item) {
            return new EventKey(item.SourceTable, item.OperationCod, item.ItemNumber, item.MovementEvent);
        }
    }

    private record DocumentItemKey(String sourceTable, String operationCod, int itemNumber) {
        static DocumentItemKey from(KardexEntity item) {
            return new DocumentItemKey(item.SourceTable, item.OperationCod, item.ItemNumber);
        }

        static DocumentItemKey from(KardexZoneEntity item) {
            return new DocumentItemKey(item.SourceTable, item.OperationCod, item.ItemNumber);
        }
    }

    private record ProductKey(String productCod, String variant, String storeCod) {
    }

    private record ProductStoreKey(String productCod, String storeCod) {
    }

    private record StockKey(String productCod, String variant, String storeCod, String warehouseCod) {
        static StockKey from(KardexEntity item) {
            return new StockKey(item.ProductCod, item.Variant, item.StoreCod, item.WarehouseCod);
        }

        static StockKey from(KardexZoneEntity item) {
            return new StockKey(item.ProductCod, item.Variant, item.StoreCod, item.WarehouseCod);
        }
    }

    private record ZoneStockKey(
            String productCod, String variant, String storeCod, String warehouseCod, String zone
    ) {
        static ZoneStockKey from(KardexZoneEntity item) {
            return new ZoneStockKey(
                    item.ProductCod, item.Variant, item.StoreCod,
                    item.WarehouseCod, item.ZoneStockMoved
            );
        }
    }

    private record TransferMovementLine(
            int itemNumber,
            String productCod,
            String variant,
            String warehouseCod,
            int quantity,
            String lotNumber,
            Date expirationDate
    ) {
    }

    private record StockDelta(int physical, int unavailable, int reserved) {
        static StockDelta from(String zone, int quantity) {
            return switch (zone) {
                case KardexZoneConstants.ZONE_PHYSICAL -> new StockDelta(quantity, 0, 0);
                case KardexZoneConstants.ZONE_UNAVAILABLE -> new StockDelta(0, quantity, 0);
                case KardexZoneConstants.ZONE_RESERVED -> new StockDelta(0, 0, quantity);
                default -> throw new KardexZoneException("Zona de stock no soportada");
            };
        }

        int total() {
            return this.physical + this.unavailable + this.reserved;
        }
    }
}
