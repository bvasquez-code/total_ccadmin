package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.repository.WarehouseRepository;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferDetRegisterMassiveDto;
import com.ccadmin.app.transfer.model.dto.TransferDispatchDto;
import com.ccadmin.app.transfer.model.dto.TransferReceiveDto;
import com.ccadmin.app.transfer.model.dto.TransferRegisterBundleDto;
import com.ccadmin.app.transfer.model.entity.CarrierEntity;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;
import com.ccadmin.app.transfer.repository.CarrierRepository;
import com.ccadmin.app.transfer.repository.TransferDetRepository;
import com.ccadmin.app.transfer.repository.TransferDocumentRepository;
import com.ccadmin.app.transfer.repository.TransferHeadRepository;
import com.ccadmin.app.system.utility.StringUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransferCreateService extends SessionService {

    @Autowired
    private TransferHeadRepository transferHeadRepository;
    @Autowired
    private TransferDetRepository transferDetRepository;
    @Autowired
    private TransferDocumentRepository transferDocumentRepository;
    @Autowired
    private CarrierRepository carrierRepository;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private CounterfoilShared counterfoilShared;

    public TransferRegisterBundleDto create(TransferRegisterBundleDto request) throws Exception {
        return this.save(request, true);
    }

    public TransferRegisterBundleDto update(TransferRegisterBundleDto request) throws Exception {
        return this.save(request, false);
    }

    public String createCode(String storeCod){
        return this.transferHeadRepository.getTransferCod(storeCod);
    }

    private TransferRegisterBundleDto save(TransferRegisterBundleDto request, boolean isCreate) throws Exception {
        if (request == null || request.transferHead == null) {
            throw new TransferException("Información de transferencia es obligatoria");
        }

        TransferHeadEntity head = request.transferHead;
        String typeOperation = head.TypeOperation;

        if (StringUtil.isEmpty(typeOperation)) {
            throw new TransferException("Tipo de operación es obligatorio");
        }

        boolean isNew = (head.TransferCod == null || head.TransferCod.trim().isEmpty());
        if (isNew) {
            throw new TransferException("TransferCod es obligatorio");
        }

        if (TransferConstants.TYPE_OPERATION_REQUEST.equals(typeOperation)) {
            if (StringUtil.isEmpty(head.TransferStatus)) {
                head.TransferStatus = TransferConstants.STATUS_PENDING;
            }
        }

        if (TransferConstants.TYPE_OPERATION_SEND.equals(typeOperation)) {
            TransferHeadEntity requestHead = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                    head.TransferCod, TransferConstants.TYPE_OPERATION_REQUEST
            );
            if (requestHead != null) {
                if (StringUtil.isEmpty(head.StoreCodOrigin)) {
                    head.StoreCodOrigin = requestHead.StoreCodOrigin;
                }
                if (StringUtil.isEmpty(head.StoreCodDest)) {
                    head.StoreCodDest = requestHead.StoreCodDest;
                }
                if (StringUtil.isEmpty(head.StoreCodRequestedBy)) {
                    head.StoreCodRequestedBy = requestHead.StoreCodRequestedBy;
                }
            }
            if (StringUtil.isEmpty(head.TransferStatus)) {
                head.TransferStatus = TransferConstants.STATUS_PENDING;
            }

            TransferHeadEntity existing = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                    head.TransferCod, TransferConstants.TYPE_OPERATION_SEND
            );
            if (existing != null) {
                if (TransferConstants.STATUS_CONFIRMED.equals(existing.TransferStatus)) {
                    throw new TransferException("No se puede modificar transferencia ya despachada o finalizada");
                }
                isNew = false;
            }
        }

        head.validate();

        if (request.transferDetList == null || request.transferDetList.isEmpty()) {
            throw new TransferException("Detalle de transferencia es obligatorio");
        }

        List<TransferDetEntity> detList = this.prepareDetails(request.transferDetList, head);

        if (TransferConstants.TYPE_OPERATION_SEND.equals(typeOperation)) {
            this.validateTsAgainstTe(detList, head.TransferCod, request.allowPartial);
        }

        if (!isNew) {
            this.transferDetRepository.updateStatusAll(head.TransferCod, head.TypeOperation, "I");
        }

        head.addSession(getUserCod(), isNew);
        this.transferHeadRepository.save(head);
        this.transferDetRepository.saveAll(detList);

        request.transferHead = head;
        request.transferDetList = detList;
        return request;
    }

    public String delete(String transferCod) throws Exception {
        if (StringUtil.isEmpty(transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        List<TransferHeadEntity> headList = this.transferHeadRepository.findByTransferCod(transferCod);
        if (headList == null || headList.isEmpty()) {
            throw new TransferException("Transferencia no encontrada");
        }

        for (var head : headList) {
            head.inactive(getUserCod());
            this.transferHeadRepository.save(head);
            this.transferDetRepository.updateStatusAll(transferCod, head.TypeOperation, "I");
        }

        List<TransferDocumentEntity> documents = this.transferDocumentRepository.findByTransferCodAndTypeOperation(
                transferCod, TransferConstants.TYPE_OPERATION_SEND
        );
        for (var document : documents) {
            document.inactive(getUserCod());
            this.transferDocumentRepository.save(document);
        }

        return "Transferencia eliminada correctamente";
    }

    @Transactional
    public ResponseWsDto dispatchTransfer(TransferDispatchDto request) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferHeadEntity head = this.transferHeadRepository.findById(
                request.transferCod
        ).orElse(null);

        if (head == null) {
            throw new TransferException("No existe transferencia TS para despacho");
        }

        if (TransferConstants.STATUS_CONFIRMED.equals(head.TransferStatus)) {
            return new ResponseWsDto("La transferencia ya fue despachada");
        }
        if (TransferConstants.STATUS_CANCELLED.equals(head.TransferStatus)
                || TransferConstants.STATUS_REJECTED.equals(head.TransferStatus)) {
            throw new TransferException("Transferencia anulada o rechazada");
        }

        this.validateTransport(request);

        List<TransferDetEntity> detList = this.transferDetRepository.findByTransferCod(
                request.transferCod
        );

        if (detList.isEmpty()) {
            throw new TransferException("Detalle de transferencia TS no encontrado");
        }

        int nextItemNumber = this.transferDetRepository.findMaxItemNumber(request.transferCod) + 1;

        for(var detRequest : request.detailListRequest){
            detRequest.validate();
            Optional<TransferDetEntity> detOptional = detList.stream()
                    .filter(e -> e.ItemNumber == detRequest.ItemNumber)
                    .findFirst();

            if (detOptional.isPresent()) {
                TransferDetEntity det = detOptional.get();
                det.NumUnit = detRequest.NumUnit > 0 ? detRequest.NumUnit : det.NumUnit;
                det.NumUnitDispatch = detRequest.NumUnitDispatch;
                det.LotNumber = detRequest.LotNumber;
                det.ExpirationDate = detRequest.ExpirationDate;
            } else if (detRequest.NumUnitDispatch > 0) {
                TransferDetEntity det = new TransferDetEntity();
                det.TransferCod = request.transferCod;
                det.TypeOperation = detRequest.TypeOperation;
                det.ProductCod = detRequest.ProductCod;
                det.Variant = detRequest.Variant;
                det.ItemNumber = nextItemNumber++;
                det.WarehouseCodOrigin = detRequest.WarehouseCodOrigin;
                det.WarehouseCodDest = detRequest.WarehouseCodDest;
                det.NumUnit = detRequest.NumUnit > 0 ? detRequest.NumUnit : detRequest.NumUnitDispatch;
                det.NumUnitDispatch = detRequest.NumUnitDispatch;
                det.NumUnitReception = 0;
                det.FlgRequested = detRequest.FlgRequested;
                det.LotNumber = detRequest.LotNumber;
                det.ExpirationDate = detRequest.ExpirationDate;
                det.addSession(getUserSession(request.user), true);
                detList.add(det);
            }
        }

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();


        List<TransferDetEntity> detListDispatch = detList.stream()
                .filter( e -> e.NumUnitDispatch > 0)
                .toList();

        for (var det : detListDispatch) {
            String warehouseCodOrigin = resolveWarehouse(head.StoreCodOrigin, det.WarehouseCodOrigin);
            det.WarehouseCodOrigin = warehouseCodOrigin;

            String stockKey = this.stockKey(det.ProductCod, det.Variant, head.StoreCodOrigin, warehouseCodOrigin);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(det.ProductCod, det.Variant, warehouseCodOrigin, head.StoreCodOrigin)
            );

            int stockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;
            if (stockBefore < det.NumUnitDispatch) {
                throw new TransferException("Stock insuficiente para el producto "+det.ProductCod);
            }

            KardexEntity kardex = new KardexEntity();
            kardex.OperationCod = head.TransferCod;
            kardex.ItemNumber = det.ItemNumber;
            kardex.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
            kardex.TypeOperation = TransferConstants.KARDEX_TYPE_OUT;
            kardex.ProductCod = det.ProductCod;
            kardex.Variant = det.Variant;
            kardex.StoreCod = head.StoreCodOrigin;
            kardex.WarehouseCod = warehouseCodOrigin;
            kardex.NumStockBefore = stockBefore;
            kardex.NumStockMoved = det.NumUnitDispatch;
            kardex.NumStockAfter = stockBefore - det.NumUnitDispatch;
            kardex.LotNumber = det.LotNumber;
            kardex.ExpirationDate = det.ExpirationDate;
            kardex.TypeOperationCod = 5;
            kardex.session(getUserSession(request.user));
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        TransferDocumentEntity transferDocument = this.counterfoilShared.generateDocumentTransfer(
                head.StoreCodOrigin,
                TransferConstants.DOCUMENT_TYPE_TRANSFER,
                head.TransferCod
        );

        StoreEntity storeOrigin = this.storeShared.findById(head.StoreCodOrigin);
        StoreEntity storeDest = this.storeShared.findById(head.StoreCodDest);

        if (storeOrigin == null || StringUtil.isEmpty(storeOrigin.Address)) {
            throw new TransferException("Dirección de punto de partida es obligatoria");
        }
        if (storeDest == null || StringUtil.isEmpty(storeDest.Address)) {
            throw new TransferException("Dirección de punto de llegada es obligatoria");
        }

        transferDocument.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
        transferDocument.DocumentRole = "R";
        transferDocument.ReasonTransferCod = request.reasonTransferCod;
        transferDocument.TransportModeCod = request.transportModeCod;
        transferDocument.DepartureAddress = storeOrigin.Address;
        transferDocument.DepartureUbigeo = storeOrigin.UbigeoCod;
        transferDocument.ArrivalAddress = storeDest.Address;
        transferDocument.ArrivalUbigeo = storeDest.UbigeoCod;
        transferDocument.VehiclePlate = request.vehiclePlate;
        transferDocument.DriverDocType = request.driverDocType;
        transferDocument.DriverDocNumber = request.driverDocNumber;
        transferDocument.DriverLicenseNumber = request.driverLicenseNumber;
        transferDocument.CarrierRuc = request.carrierRuc;
        transferDocument.CarrierName = request.carrierName;

        transferDocument.validate().session(getUserSession(request.user));

        CarrierEntity carrier = new CarrierEntity();
        carrier.CarrierCod = request.driverDocNumber;
        carrier.CarrierRuc = request.carrierRuc;
        carrier.CarrierName = request.carrierName;
        carrier.VehiclePlate = request.vehiclePlate;
        carrier.DriverDocType = request.driverDocType;
        carrier.DriverDocNumber = request.driverDocNumber;
        carrier.DriverLicenseNumber = request.driverLicenseNumber;
        carrier.addSession(getUserCod());
        
        Date now = new Date();
        head.DispatchDate = now;
        head.UserOriginConfirm = getUserSession(request.user);
        head.DateOriginConfirm = now;
        head.TransferStatus = TransferConstants.STATUS_CONFIRMED;
        if (StringUtil.isNotEmpty(request.observation)) {
            head.Observation = request.observation;
        }
        head.addSession(getUserSession(request.user), false);

        this.transferHeadRepository.save(head);
        this.transferDetRepository.saveAll(detList);
        this.kardexShared.saveAll(kardexList);
        this.transferDocumentRepository.save(transferDocument);
        this.carrierRepository.save(carrier);

        return new ResponseWsDto("Transferencia despachada correctamente");
    }

    @Transactional
    public ResponseWsDto receiveTransfer(TransferReceiveDto request) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferHeadEntity head = this.transferHeadRepository.findById(
                request.transferCod
        ).orElse(null);
        if (head == null) {
            throw new TransferException("No existe transferencia TE para recepción");
        }

        if (TransferConstants.STATUS_CONFIRMED.equals(head.ReceiveStatus)) {
            return new ResponseWsDto("La transferencia ya fue recibida");
        }

        List<TransferDetEntity> detList = this.transferDetRepository.findByTransferCod(
                request.transferCod
        );
        if (detList.isEmpty()) {
            throw new TransferException("Detalle de transferencia TS no encontrado");
        }

        for(var detRequest : request.detailListReceive.stream()
                .filter( e -> StringUtil.nvl(e.FlgRequested,"S").equals("S") ).toList()){
            detList.stream()
                    .filter(e -> e.ItemNumber == detRequest.ItemNumber)
                    .findFirst()
                    .ifPresent(det -> det.NumUnitReception = detRequest.NumUnitReception);
        }

        for(var detRequest : request.detailListReceive.stream()
                .filter( e -> StringUtil.nvl(e.FlgRequested,"S").equals("N") ).toList()){
            detRequest.addSession(getUserCod());
            detList.add(detRequest);
        }

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        List<TransferDetEntity> detListReceive = detList.stream()
                .filter( e -> e.NumUnitReception > 0)
                .toList();

        for (var det : detListReceive) {
            String warehouseCodDest = resolveWarehouse(head.StoreCodDest, det.WarehouseCodDest);
            det.WarehouseCodDest = warehouseCodDest;

            String stockKey = this.stockKey(det.ProductCod, det.Variant, head.StoreCodDest, warehouseCodDest);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(det.ProductCod, det.Variant, warehouseCodDest, head.StoreCodDest)
            );

            int stockBefore = (kardexLast == null) ? 0 : kardexLast.NumStockAfter;

            KardexEntity kardex = new KardexEntity();
            kardex.OperationCod = head.TransferCod;
            kardex.ItemNumber = det.ItemNumber;
            kardex.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
            kardex.TypeOperation = TransferConstants.KARDEX_TYPE_IN;
            kardex.ProductCod = det.ProductCod;
            kardex.Variant = det.Variant;
            kardex.StoreCod = head.StoreCodDest;
            kardex.WarehouseCod = warehouseCodDest;
            kardex.NumStockBefore = stockBefore;
            kardex.NumStockMoved = det.NumUnitReception;
            kardex.NumStockAfter = stockBefore + det.NumUnitReception;
            kardex.LotNumber = det.LotNumber;
            kardex.ExpirationDate = det.ExpirationDate;
            kardex.TypeOperationCod = 6;
            kardex.session(getUserSession(request.user));
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        Date now = new Date();
        head.ArrivalDate = now;
        head.UserDestConfirm = getUserSession(request.user);
        head.DateDestConfirm = now;
        head.ReceiveStatus = TransferConstants.STATUS_CONFIRMED;
        if (StringUtil.isNotEmpty(request.observation)) {
            head.Observation = request.observation;
        }
        head.addSession(getUserSession(request.user), false);

        this.transferHeadRepository.save(head);
        this.transferDetRepository.saveAll(detList);
        this.kardexShared.saveAll(kardexList);

        return new ResponseWsDto("Transferencia recibida correctamente");
    }

    public ResponseWsDto rejectTransfer(TransferReceiveDto request) throws Exception {
        return this.changeStatus(request, TransferConstants.STATUS_REJECTED, "Transferencia rechazada correctamente");
    }

    public ResponseWsDto cancelTransfer(TransferReceiveDto request) throws Exception {
        return this.changeStatus(request, TransferConstants.STATUS_CANCELLED, "Transferencia anulada correctamente");
    }

    public ResponseWsDto approvedTransfer(TransferReceiveDto request) throws Exception {
        return this.changeStatus(request, TransferConstants.STATUS_APPROVED, "Transferencia aprobada correctamente");
    }

    private ResponseWsDto changeStatus(TransferReceiveDto request, String status, String message) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferHeadEntity headRequest = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                request.transferCod, request.typeOperation
        );
        if (headRequest == null) {
            throw new TransferException("No existe solicitud TE para la transferencia");
        }

        TransferHeadEntity headSend = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                request.transferCod, TransferConstants.TYPE_OPERATION_SEND
        );

        if (TransferConstants.STATUS_CONFIRMED.equals(headRequest.TransferStatus)) {
            throw new TransferException("No se puede modificar transferencia ya despachada");
        }
        if (TransferConstants.STATUS_CONFIRMED.equals(headSend.TransferStatus)) {
            throw new TransferException("No se puede modificar transferencia ya despachada");
        }

        headRequest.TransferStatus = status;
        if (StringUtil.isNotEmpty(request.observation)) {
            headRequest.Observation = request.observation;
        }
        headRequest.addSession(getUserSession(request.user), false);
        this.transferHeadRepository.save(headRequest);

        if (headSend != null) {
            headSend.TransferStatus = status;
            if (StringUtil.isNotEmpty(request.observation)) {
                headSend.Observation = request.observation;
            }
            headSend.addSession(getUserSession(request.user), false);
            this.transferHeadRepository.save(headSend);
        }

        return new ResponseWsDto(message);
    }

    private List<TransferDetEntity> prepareDetails(List<TransferDetEntity> detList, TransferHeadEntity head)
            throws Exception {
        List<TransferDetEntity> detailList = new ArrayList<>();
        int itemNumber = 1;

        for (var det : detList) {
            det.TransferCod = head.TransferCod;
            det.TypeOperation = head.TypeOperation;
            if (StringUtil.isEmpty(det.Variant)) {
                det.Variant = "0000";
            }
            if (det.ItemNumber <= 0) {
                det.ItemNumber = itemNumber++;
            }

            det.validate();

            ProductEntity product = this.productShared.findById(det.ProductCod);
            if (product == null || !"A".equals(product.Status)) {
                throw new TransferException("Producto inválido o inactivo: "+det.ProductCod);
            }

            if (StringUtil.isNotEmpty(det.WarehouseCodOrigin)) {
                WarehouseEntity whOrigin = this.warehouseRepository.findById(det.WarehouseCodOrigin)
                        .orElseThrow(() -> new TransferException("Almacén origen no existe"));
                if (!"A".equals(whOrigin.Status)) {
                    throw new TransferException("Almacén origen inactivo");
                }
            }
            if (StringUtil.isEmpty(det.WarehouseCodOrigin)) {
                det.WarehouseCodOrigin = this.warehouseRepository.findByStore(head.StoreCodOrigin)
                        .stream().findFirst()
                        .get().WarehouseCod;
            }

            if (StringUtil.isNotEmpty(det.WarehouseCodDest)) {
                WarehouseEntity whDest = this.warehouseRepository.findById(det.WarehouseCodDest)
                        .orElseThrow(() -> new TransferException("Almacén destino no existe"));
                if (!"A".equals(whDest.Status)) {
                    throw new TransferException("Almacén destino inactivo");
                }
            }
            if (StringUtil.isEmpty(det.WarehouseCodDest)) {
                det.WarehouseCodDest = this.warehouseRepository.findByStore(head.StoreCodDest)
                        .stream().findFirst()
                        .get().WarehouseCod;
            }

            det.addSession(getUserCod(), det.CreationUser == null || det.CreationUser.isEmpty());
            detailList.add(det);
        }

        return detailList;
    }

    private void validateTsAgainstTe(List<TransferDetEntity> detList, String transferCod, Boolean allowPartial)
            throws Exception {
        List<TransferDetEntity> requestDetList = this.transferDetRepository.findByTransferCodAndTypeOperation(
                transferCod, TransferConstants.TYPE_OPERATION_REQUEST
        );

        if (requestDetList.isEmpty()) {
            return;
        }

        Map<String, Integer> teQtyByProduct = new HashMap<>();
        for (var det : requestDetList) {
            String key = det.ProductCod+"-"+det.Variant;
            teQtyByProduct.put(key, teQtyByProduct.getOrDefault(key, 0) + det.NumUnit);
        }

        Map<String, Integer> tsQtyByProduct = new HashMap<>();
        for (var det : detList) {
            String key = det.ProductCod+"-"+det.Variant;
            tsQtyByProduct.put(key, tsQtyByProduct.getOrDefault(key, 0) + det.NumUnit);
        }

        boolean hasPartial = false;

        for (var entry : tsQtyByProduct.entrySet()) {
            Integer teQty = teQtyByProduct.get(entry.getKey());
            if (teQty == null) {
                throw new TransferException("El detalle TS contiene productos que no existen en TE");
            }
            if (entry.getValue() > teQty) {
                throw new TransferException("La cantidad TS no puede exceder a TE");
            }
            if (entry.getValue() < teQty) {
                hasPartial = true;
            }
        }

        if (hasPartial && (allowPartial == null || !allowPartial)) {
            throw new TransferException("La transferencia parcial requiere confirmación explícita");
        }
    }

    private void validateTransport(TransferDispatchDto request) throws Exception {
        if (StringUtil.isEmpty(request.transportModeCod)) {
            throw new TransferException("Modalidad de transporte es obligatoria");
        }
        if (TransferConstants.TRANSPORT_PUBLIC.equals(request.transportModeCod)) {
            if (StringUtil.isEmpty(request.carrierRuc) || StringUtil.isEmpty(request.carrierName)) {
                throw new TransferException("Transportista es obligatorio para transporte público");
            }
        }
        if (TransferConstants.TRANSPORT_PRIVATE.equals(request.transportModeCod)) {
            if (StringUtil.isEmpty(request.vehiclePlate)
                    || StringUtil.isEmpty(request.driverDocType)
                    || StringUtil.isEmpty(request.driverDocNumber)
                    || StringUtil.isEmpty(request.driverLicenseNumber)) {
                throw new TransferException("Datos del conductor y vehículo son obligatorios para transporte privado");
            }
        }
    }

    private String resolveWarehouse(String storeCod, String warehouseCod) throws Exception {
        if (StringUtil.isNotEmpty(warehouseCod)) {
            WarehouseEntity warehouse = this.warehouseRepository.findById(warehouseCod)
                    .orElseThrow(() -> new TransferException("Almacén no existe"));
            if (!"A".equals(warehouse.Status)) {
                throw new TransferException("Almacén inactivo");
            }
            return warehouseCod;
        }

        List<WarehouseEntity> warehouses = this.warehouseRepository.findByStore(storeCod);
        if (warehouses.size() == 1) {
            return warehouses.getFirst().WarehouseCod;
        }

        throw new TransferException("Debe indicar almacén para el local");
    }

    private String getUserSession(String user) {
        return StringUtil.isEmpty(user) ? getUserCod() : user;
    }

    private String stockKey(String productCod, String variant, String storeCod, String warehouseCod) {
        return productCod + "|" + variant + "|" + storeCod + "|" + warehouseCod;
    }

    public TransferDetRegisterMassiveDto saveDet(TransferDetRegisterMassiveDto transferDetRegisterMassiveDto) throws TransferException {

        TransferDetEntity transferDet = transferDetRegisterMassiveDto.transferDetList.getFirst();

        TransferHeadEntity transferHead = this.transferHeadRepository.findById(transferDet.TransferCod).orElse(null);

        if(transferHead == null){
            throw new TransferException("Transferencia no encontrada");
        }

        if(transferHead.TransferStatus.equals(TransferConstants.STATUS_CONFIRMED)
                && transferHead.ReceiveStatus.equals(TransferConstants.STATUS_CONFIRMED)){
            throw new TransferException("Transferencia ya se encuentra completamente cerrada.");
        }

        for(var item : transferDetRegisterMassiveDto.transferDetList){
            item.addSession(getUserCod());
        }

        List<TransferDetEntity> transferDetListDB = this.transferDetRepository.saveAll(transferDetRegisterMassiveDto.transferDetList);

        transferDetListDB.stream().forEach(e -> {
            e.Product = this.productShared.findById(e.ProductCod);
        });

        return new TransferDetRegisterMassiveDto(transferDetListDB);
    }
}
