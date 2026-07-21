package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.repository.WarehouseRepository;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import com.ccadmin.app.system.utility.StringUtil;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferDispatchDto;
import com.ccadmin.app.transfer.model.dto.TransferReceiveDto;
import com.ccadmin.app.transfer.model.dto.TransferRequestRegisterBundleDto;
import com.ccadmin.app.transfer.model.entity.*;
import com.ccadmin.app.transfer.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransferRequestCreateService extends SessionService {

    @Autowired
    private TransferRequestHeadRepository transferRequestHeadRepository;
    @Autowired
    private TransferRequestDetRepository transferRequestDetRepository;
    @Autowired
    private TransferDocumentRepository transferDocumentRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private CounterfoilShared counterfoilShared;
    @Autowired
    private TransferRequestDetService transferRequestDetService;

    public String createCode(String storeCod){
        return this.transferRequestHeadRepository.getTransferCod(storeCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public TransferRequestRegisterBundleDto save(TransferRequestRegisterBundleDto request) throws Exception {
        if (request == null || request.transferHead == null) {
            throw new TransferException("Información de transferencia es obligatoria");
        }

        TransferRequestHeadEntity head = request.transferHead;
        String typeOperation = head.TypeOperation;

        if (StringUtil.isEmpty(typeOperation)) {
            throw new TransferException("Tipo de operación es obligatorio");
        }

        if (StringUtil.isEmpty(head.TransferReqCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }
        boolean isNew = !this.transferRequestHeadRepository.existsById(head.TransferReqCod);

        if (TransferConstants.TYPE_OPERATION_REQUEST.equals(typeOperation)) {
            if (!isNew) {
                TransferRequestHeadEntity existingHead = this.transferRequestHeadRepository.findById(head.TransferReqCod)
                        .orElseThrow(() -> new TransferException("Solicitud de transferencia no encontrada"));
                if (!TransferConstants.isEditableRequestStatus(existingHead.TransferStatus)
                        || !"A".equals(existingHead.Status)) {
                    throw new TransferException("La solicitud de transferencia ya no se puede editar");
                }
                head.TransferStatus = existingHead.TransferStatus;
                head.CreationUser = existingHead.CreationUser;
                head.CreationDate = existingHead.CreationDate;
                head.Status = existingHead.Status;
            }
            if (StringUtil.isEmpty(head.TransferStatus)) {
                head.TransferStatus = TransferConstants.STATUS_PENDING;
            }
        }

        if (TransferConstants.TYPE_OPERATION_SEND.equals(typeOperation)) {
            TransferRequestHeadEntity requestHead = this.transferRequestHeadRepository.findByTransferCodAndTypeOperation(
                    head.TransferReqCod, TransferConstants.TYPE_OPERATION_REQUEST
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

            TransferRequestHeadEntity existing = this.transferRequestHeadRepository.findByTransferCodAndTypeOperation(
                    head.TransferReqCod, TransferConstants.TYPE_OPERATION_SEND
            );
            if (existing != null) {
                if (TransferConstants.STATUS_CONFIRMED.equals(existing.TransferStatus)) {
                    throw new TransferException("No se puede modificar transferencia ya despachada o finalizada");
                }
                isNew = false;
            }
        }

        if (StringUtil.isEmpty(head.ReceiveStatus)) {
            head.ReceiveStatus = TransferConstants.STATUS_PENDING;
        }

        head.validate();

        if (request.transferDetList == null || request.transferDetList.isEmpty()) {
            throw new TransferException("Detalle de transferencia es obligatorio");
        }

        if (!isNew) {
            this.transferRequestDetRepository.updateStatusAll(head.TransferReqCod, head.TypeOperation, "I");
        }

        List<TransferRequestDetEntity> detList = this.prepareDetails(request.transferDetList, head);

        if (TransferConstants.TYPE_OPERATION_SEND.equals(typeOperation)) {
            this.validateTsAgainstTe(detList, head.TransferReqCod, request.allowPartial);
        }

        head.addSession(getUserCod(), isNew);
        this.transferRequestHeadRepository.save(head);
        this.transferRequestDetRepository.saveAll(detList);

        request.transferHead = head;
        request.transferDetList = detList;
        return request;
    }

    public String delete(String transferCod) throws Exception {
        if (StringUtil.isEmpty(transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        List<TransferRequestHeadEntity> headList = this.transferRequestHeadRepository.findByTransferCod(transferCod);
        if (headList == null || headList.isEmpty()) {
            throw new TransferException("Transferencia no encontrada");
        }

        for (var head : headList) {
            head.inactive(getUserCod());
            this.transferRequestHeadRepository.save(head);
            this.transferRequestDetRepository.updateStatusAll(transferCod, head.TypeOperation, "I");
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

    @Transactional(rollbackOn = Exception.class)
    public ResponseWsDto dispatchTransfer(TransferDispatchDto request) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferRequestHeadEntity head = this.transferRequestHeadRepository.findByTransferCodAndTypeOperationForUpdate(
                request.transferCod, TransferConstants.TYPE_OPERATION_SEND
        );
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

        List<TransferRequestDetEntity> detList = this.transferRequestDetRepository.findByTransferCodAndTypeOperation(
                request.transferCod, TransferConstants.TYPE_OPERATION_SEND
        );
        if (detList.isEmpty()) {
            throw new TransferException("Detalle de transferencia TS no encontrado");
        }

        for (var det : detList) {
            String warehouseCodOrigin = resolveWarehouse(head.StoreCodOrigin, det.WarehouseCodOrigin);
            det.WarehouseCodOrigin = warehouseCodOrigin;
        }

        List<KardexEntity> kardexList = this.kardexShared.buildTransferRequestDispatch(
                head.TransferReqCod, head.StoreCodOrigin,
                detList, getUserSession(request.user)
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZoneTransferRequestDispatch(
                head.TransferReqCod, head.StoreCodOrigin,
                detList, getUserSession(request.user)
        );

        TransferDocumentEntity transferDocument = this.counterfoilShared.generateDocumentTransfer(
                head.StoreCodOrigin,
                TransferConstants.DOCUMENT_TYPE_TRANSFER,
                head.TransferReqCod
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

        Date now = new Date();
        head.DispatchDate = now;
        head.UserOriginConfirm = getUserSession(request.user);
        head.DateOriginConfirm = now;
        head.TransferStatus = TransferConstants.STATUS_CONFIRMED;
        if (StringUtil.isNotEmpty(request.observation)) {
            head.Observation = request.observation;
        }
        head.addSession(getUserSession(request.user), false);

        this.transferRequestHeadRepository.save(head);
        this.transferDocumentRepository.save(transferDocument);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

        return new ResponseWsDto("Transferencia despachada correctamente");
    }

    @Transactional(rollbackOn = Exception.class)
    public ResponseWsDto receiveTransfer(TransferReceiveDto request) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferRequestHeadEntity head = this.transferRequestHeadRepository.findByTransferCodAndTypeOperationForUpdate(
                request.transferCod,
                TransferConstants.TYPE_OPERATION_SEND
        );
        if (head == null) {
            throw new TransferException("No existe transferencia TE para recepción");
        }

        if (!TransferConstants.STATUS_CONFIRMED.equals(head.TransferStatus)) {
            throw new TransferException("La transferencia aun no fue despachada");
        }
        if (TransferConstants.STATUS_CONFIRMED.equals(head.ReceiveStatus)) {
            return new ResponseWsDto("La transferencia ya fue recibida");
        }

        List<TransferRequestDetEntity> detList = this.transferRequestDetRepository.findByTransferCodAndTypeOperation(
                request.transferCod,
                TransferConstants.TYPE_OPERATION_SEND
        );
        if (detList.isEmpty()) {
            throw new TransferException("Detalle de transferencia TS no encontrado");
        }

        if (request.detailListReceive != null) {
            for (TransferDetEntity received : request.detailListReceive) {
                detList.stream()
                        .filter(detail -> detail.ItemNumber == received.ItemNumber)
                        .findFirst()
                        .ifPresent(detail -> detail.NumUnitReception = received.NumUnitReception);
            }
        }

        List<TransferRequestDetEntity> detListReceive = detList.stream()
                .filter(detail -> detail.NumUnitReception > 0)
                .toList();

        for (TransferRequestDetEntity detail : detList) {
            if (detail.NumUnitReception < 0) {
                throw new TransferException("La cantidad recibida no puede ser negativa");
            }
            if (detail.NumUnitReception > detail.NumUnit) {
                throw new TransferException(
                        "La cantidad recibida supera lo despachado para el item " + detail.ItemNumber
                );
            }
        }

        for (var det : detListReceive) {
            String warehouseCodDest = resolveWarehouse(head.StoreCodDest, det.WarehouseCodDest);
            det.WarehouseCodDest = warehouseCodDest;
        }

        List<KardexEntity> kardexList = this.kardexShared.buildTransferRequestReceipt(
                head.TransferReqCod, head.StoreCodDest,
                detListReceive, getUserSession(request.user)
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZoneTransferRequestReceipt(
                head.TransferReqCod, head.StoreCodDest,
                detListReceive, getUserSession(request.user)
        );

        Date now = new Date();
        head.ArrivalDate = now;
        head.UserDestConfirm = getUserSession(request.user);
        head.DateDestConfirm = now;
        head.ReceiveStatus = TransferConstants.STATUS_CONFIRMED;
        if (StringUtil.isNotEmpty(request.observation)) {
            head.Observation = request.observation;
        }
        head.addSession(getUserSession(request.user), false);

        this.transferRequestHeadRepository.save(head);
        this.transferRequestDetRepository.saveAll(detList);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

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

    public ResponseWsDto inReviewTransfer(TransferReceiveDto request) throws Exception {
        return this.changeStatus(request, TransferConstants.STATUS_IN_REVIEW, "Transferencia pasada a la espera de revisión");
    }

    private ResponseWsDto changeStatus(TransferReceiveDto request, String status, String message) throws Exception {
        if (request == null || StringUtil.isEmpty(request.transferCod)) {
            throw new TransferException("TransferCod es obligatorio");
        }

        TransferRequestHeadEntity headRequest = this.transferRequestHeadRepository.findById(
                request.transferCod
        ).orElse(null);

        if (headRequest == null) {
            throw new TransferException("No existe solicitud TE para la transferencia");
        }

        if (TransferConstants.STATUS_CONFIRMED.equals(headRequest.TransferStatus)) {
            throw new TransferException("No se puede modificar transferencia ya despachada");
        }

        headRequest.TransferStatus = status;
        if (StringUtil.isNotEmpty(request.observation)) {
            headRequest.Observation = request.observation;
        }
        headRequest.addSession(getUserSession(request.user), false);
        headRequest.TransferStatus = status;
        if (StringUtil.isNotEmpty(request.observation)) {
            headRequest.Observation = request.observation;
        }
        headRequest.addSession(getUserSession(request.user), false);
        this.transferRequestHeadRepository.save(headRequest);

        return new ResponseWsDto(message);
    }

    private List<TransferRequestDetEntity> prepareDetails(List<TransferRequestDetEntity> detList, TransferRequestHeadEntity head)
            throws Exception {
        List<TransferRequestDetEntity> detailList = new ArrayList<>();
        Set<Integer> usedItemNumbers = new HashSet<>();
        detList.stream()
                .filter(det -> det.ItemNumber > 0)
                .forEach(det -> usedItemNumbers.add(det.ItemNumber));

        int nextItemNumber = 1;

        for (var det : detList) {
            if (det.ItemNumber <= 0) {
                while (usedItemNumbers.contains(nextItemNumber)) {
                    nextItemNumber++;
                }
                det.ItemNumber = nextItemNumber;
                usedItemNumbers.add(nextItemNumber);
            }
            detailList.add(this.transferRequestDetService.buildDetailToSave(det, head, det.ItemNumber));
        }

        return detailList;
    }

    private void validateTsAgainstTe(List<TransferRequestDetEntity> detList, String transferCod, Boolean allowPartial)
            throws Exception {
        List<TransferRequestDetEntity> requestDetList = this.transferRequestDetRepository.findByTransferCodAndTypeOperation(
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

    public ResponseWsDto confirmedTransfer(TransferReceiveDto request) throws Exception {
        return this.changeStatus(request, TransferConstants.STATUS_CONFIRMED, "Transferencia aprobada correctamente");
    }
}
