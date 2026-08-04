package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.repository.WarehouseRepository;
import com.ccadmin.app.system.utility.StringUtil;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferRequestDetSaveDto;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestHeadEntity;
import com.ccadmin.app.transfer.model.entity.id.TransferRequestDetId;
import com.ccadmin.app.transfer.repository.TransferRequestDetRepository;
import com.ccadmin.app.transfer.repository.TransferRequestHeadRepository;
import com.ccadmin.app.transfer.service.helper.ProductTransferConversionHelper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransferRequestDetService extends SessionService {

    @Autowired
    private TransferRequestHeadRepository transferRequestHeadRepository;
    @Autowired
    private TransferRequestDetRepository transferRequestDetRepository;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private ProductTransferConversionHelper productTransferConversionHelper;

    @Transactional
    public TransferRequestDetSaveDto save(TransferRequestDetSaveDto request) throws Exception {
        TransferRequestHeadEntity head = findEditableHead(request);
        if (request.transferDet.ItemNumber <= 0) {
            request.transferDet.ItemNumber = this.transferRequestDetRepository.findMaxItemNumber(head.TransferReqCod) + 1;
        }

        request.transferDet = this.transferRequestDetRepository.save(
                buildDetailToSave(request.transferDet, head, request.transferDet.ItemNumber)
        );
        request.transferHead = head;
        return request;
    }

    public TransferRequestDetEntity buildDetailToSave(
            TransferRequestDetEntity source,
            TransferRequestHeadEntity head,
            int itemNumber
    ) throws Exception {
        source.TransferReqCod = head.TransferReqCod;
        source.TypeOperation = head.TypeOperation;
        source.ItemNumber = source.ItemNumber > 0 ? source.ItemNumber : itemNumber;
        if (StringUtil.isEmpty(source.Variant)) {
            source.Variant = "0000";
        }

        ProductEntity product = this.productShared.findById(source.ProductCod);
        if (product == null || !"A".equals(product.Status)) {
            throw new TransferException("Producto invalido o inactivo: " + source.ProductCod);
        }

        ProductConfigEntity configOrigin = this.productOperationConfigShared.findByProduct(
                source.ProductCod, head.StoreCodOrigin
        );
        ProductConfigEntity configDestination = this.productOperationConfigShared.findByProduct(
                source.ProductCod, head.StoreCodDest
        );
        if (this.productOperationConfigShared.isDigital(configOrigin)
                || this.productOperationConfigShared.isDigital(configDestination)) {
            throw new TransferException(
                    "El producto " + source.ProductCod + " es digital y no puede transferirse"
            );
        }
        if (StringUtil.isEmpty(source.ProductUnitName)) {
            source.ProductUnitName = configDestination.ProductUnitName;
        }
        if (source.ProductUnitFactor <= 0) {
            source.ProductUnitFactor = configDestination.ProductUnitFactor;
        }

        source.validate();
        this.productTransferConversionHelper.validateInternalQuantityBetweenStoresOrThrow(
                source.ProductCod,
                source.NumUnit,
                head.StoreCodOrigin,
                head.StoreCodDest,
                configOrigin,
                configDestination
        );

        source.WarehouseCodOrigin = validateOrResolveWarehouse(source.WarehouseCodOrigin, head.StoreCodOrigin);
        source.WarehouseCodDest = validateOrResolveWarehouse(source.WarehouseCodDest, head.StoreCodDest);

        TransferRequestDetEntity detail = this.transferRequestDetRepository.findById(buildId(source)).orElse(source);
        boolean isNew = StringUtil.isEmpty(detail.CreationUser);
        detail.TransferReqCod = source.TransferReqCod;
        detail.TypeOperation = source.TypeOperation;
        detail.ItemNumber = source.ItemNumber;
        detail.ProductCod = source.ProductCod;
        detail.Variant = source.Variant;
        detail.WarehouseCodOrigin = source.WarehouseCodOrigin;
        detail.WarehouseCodDest = source.WarehouseCodDest;
        detail.NumUnit = source.NumUnit;
        detail.ProductUnitName = source.ProductUnitName;
        detail.ProductUnitFactor = source.ProductUnitFactor;
        detail.NumUnitDispatch = source.NumUnitDispatch;
        detail.NumUnitReception = source.NumUnitReception;
        detail.LotNumber = source.LotNumber;
        detail.ExpirationDate = source.ExpirationDate;
        detail.Product = source.Product != null ? source.Product : product;
        detail.Status = "A";
        detail.addSession(getUserCod(), isNew);
        return detail;
    }

    @Transactional
    public TransferRequestDetSaveDto delete(TransferRequestDetSaveDto request) throws Exception {
        TransferRequestHeadEntity head = findEditableHead(request);
        request.transferDet.TransferReqCod = head.TransferReqCod;

        TransferRequestDetEntity detail = this.transferRequestDetRepository.findById(buildId(request.transferDet))
                .orElseThrow(() -> new TransferException("Detalle de transferencia no encontrado"));
        detail.inactive(getUserCod());
        request.transferDet = this.transferRequestDetRepository.save(detail);
        request.transferHead = head;
        return request;
    }

    private TransferRequestHeadEntity findEditableHead(TransferRequestDetSaveDto request) throws TransferException {
        if (request == null || request.transferHead == null || request.transferDet == null) {
            throw new TransferException("La cabecera y el detalle son obligatorios");
        }

        String transferReqCod = request.transferHead.TransferReqCod;
        if (StringUtil.isEmpty(transferReqCod)) {
            transferReqCod = request.transferDet.TransferReqCod;
        }
        if (StringUtil.isEmpty(transferReqCod)) {
            throw new TransferException("TransferReqCod es obligatorio");
        }

        TransferRequestHeadEntity head = this.transferRequestHeadRepository.findById(transferReqCod)
                .orElseThrow(() -> new TransferException("Solicitud de transferencia no encontrada"));
        if (!TransferConstants.TYPE_OPERATION_REQUEST.equals(head.TypeOperation)
                || !TransferConstants.isEditableRequestStatus(head.TransferStatus)
                || !"A".equals(head.Status)) {
            throw new TransferException("La solicitud de transferencia ya no se puede editar");
        }
        return head;
    }

    private String validateOrResolveWarehouse(String warehouseCod, String storeCod) throws TransferException {
        if (StringUtil.isNotEmpty(warehouseCod)) {
            WarehouseEntity warehouse = this.warehouseRepository.findById(warehouseCod)
                    .orElseThrow(() -> new TransferException("Almacen no existe"));
            if (!"A".equals(warehouse.Status)) {
                throw new TransferException("Almacen inactivo");
            }
            return warehouseCod;
        }

        return this.warehouseRepository.findByStore(storeCod).stream()
                .findFirst()
                .map(warehouse -> warehouse.WarehouseCod)
                .orElseThrow(() -> new TransferException("No existe almacen para el local " + storeCod));
    }

    private TransferRequestDetId buildId(TransferRequestDetEntity detail) {
        TransferRequestDetId id = new TransferRequestDetId();
        id.TransferReqCod = detail.TransferReqCod;
        id.ItemNumber = detail.ItemNumber;
        return id;
    }
}
