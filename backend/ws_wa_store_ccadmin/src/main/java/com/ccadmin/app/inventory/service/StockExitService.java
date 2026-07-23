package com.ccadmin.app.inventory.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.*;
import com.ccadmin.app.inventory.model.entity.StockExitDetEntity;
import com.ccadmin.app.inventory.model.entity.StockExitHeadEntity;
import com.ccadmin.app.inventory.repository.StockExitDetRepository;
import com.ccadmin.app.inventory.repository.StockExitHeadRepository;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.KardexCreateService;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class StockExitService extends SessionService {
    private static final int PAGE_SIZE = 10;
    private final StockExitHeadRepository headRepository;
    private final StockExitDetRepository detRepository;
    private final StockMovementValidationService validation;
    private final BusinessConfigRepository businessConfigRepository;
    private final WarehouseRepository warehouseRepository;
    private final KardexCreateService kardexCreateService;

    public StockExitService(StockExitHeadRepository headRepository,
                            StockExitDetRepository detRepository,
                            StockMovementValidationService validation,
                            BusinessConfigRepository businessConfigRepository,
                            WarehouseRepository warehouseRepository,
                            KardexCreateService kardexCreateService) {
        this.headRepository = headRepository;
        this.detRepository = detRepository;
        this.validation = validation;
        this.businessConfigRepository = businessConfigRepository;
        this.warehouseRepository = warehouseRepository;
        this.kardexCreateService = kardexCreateService;
    }

    public ResponsePageSearchT<StockExitHeadEntity> findAll(StockMovementSearchDto request) {
        int page = Math.max(1, request.Page);
        String query = clean(request.Query);
        String status = clean(request.ProcessStatus);
        String type = StockMovementConstants.PROCESS_ORIGINAL;
        int count = headRepository.countSearch(getStoreCod(), query, status, type, request.DateStart, request.DateEnd);
        List<StockExitHeadEntity> result = headRepository.search(
                getStoreCod(), query, status, type, request.DateStart, request.DateEnd,
                (page - 1) * PAGE_SIZE, PAGE_SIZE
        );
        result.forEach(head ->
                head.HasPendingResolution = detRepository.countPendingByCode(head.StockExitCod) > 0
        );
        return new ResponsePageSearchT<>(result, page, PAGE_SIZE, count);
    }

    public StockExitRegisterDto findById(String code) {
        StockExitHeadEntity head = headRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("No existe el retiro de stock"));
        requireStore(head.StoreCod);
        StockExitRegisterDto result = new StockExitRegisterDto();
        result.Head = head;
        result.DetailList = detRepository.findByCode(code);
        return result;
    }

    public ResponseWsDto findDataForm(String code) {
        ResponseWsDto response = new ResponseWsDto();
        if (code != null && !code.isBlank()) response.AddResponseAdditional("movement", findById(code));
        response.AddResponseAdditional("reasonList", businessConfigRepository.findActivesByGroupId(9));
        response.AddResponseAdditional("unavailableReasonList", businessConfigRepository.findActivesByGroupId(10));
        response.AddResponseAdditional("releaseReasonList", businessConfigRepository.findActivesByGroupId(11));
        response.AddResponseAdditional("withdrawReasonList", businessConfigRepository.findActivesByGroupId(12));
        response.AddResponseAdditional("warehouseList", warehouseRepository.findByStore(getStoreCod()));
        return response.okResponse(null);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockExitRegisterDto save(StockExitRegisterDto request) {
        if (request == null || request.Head == null) throw new IllegalArgumentException("Documento requerido");
        StockExitHeadEntity head = request.Head;
        head.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        head.OriginStockExitCod = null;
        boolean isNew = head.StockExitCod == null || head.StockExitCod.isBlank();
        if (isNew) {
            head.StockExitCod = headRepository.createCode(getStoreCod());
            head.ProcessStatus = StatusConst.PENDING;
        } else {
            StockExitHeadEntity current = headRepository.findForUpdate(head.StockExitCod);
            if (current == null) throw new IllegalArgumentException("No existe el retiro de stock");
            requireStore(current.StoreCod);
            if (!StatusConst.PENDING.equals(current.ProcessStatus)) {
                throw new IllegalStateException("Solo se puede editar un documento pendiente");
            }
            head.CreationUser = current.CreationUser;
            head.CreationDate = current.CreationDate;
            head.ProcessStatus = current.ProcessStatus;
        }
        normalizeAndValidate(head, request.DetailList);
        head.StoreCod = getStoreCod();
        head.Status = StatusConst.ACTIVE;
        head.addSession(getUserCod());
        headRepository.save(head);
        detRepository.deleteByCode(head.StockExitCod);
        int item = 1;
        for (StockExitDetEntity detail : request.DetailList) {
            detail.StockExitCod = head.StockExitCod;
            detail.ItemNumber = item++;
            detail.Status = StatusConst.ACTIVE;
            detail.addSession(getUserCod());
            detRepository.save(detail);
        }
        return findById(head.StockExitCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockExitRegisterDto confirm(String code) {
        StockExitHeadEntity head = headRepository.findForUpdate(code);
        requirePending(head);
        List<StockExitDetEntity> details = detRepository.findByCode(code);
        if (details.isEmpty()) throw new IllegalArgumentException("Debe registrar al menos un producto");
        List<KardexEntity> totals = new ArrayList<>();
        List<KardexZoneEntity> zones = new ArrayList<>();

        if (!StockMovementConstants.PROCESS_ORIGINAL.equals(head.ProcessType)) {
            throw new IllegalStateException("No se permiten cabeceras independientes de resolucion");
        }
        confirmOriginal(head, details, totals, zones);
        kardexCreateService.saveAll(totals, zones);
        head.ProcessStatus = StatusConst.CONFIRMED;
        head.ConfirmUser = getUserCod();
        head.ConfirmDate = new Date();
        head.addSessionModify(getUserCod());
        headRepository.save(head);
        return findById(code);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockExitRegisterDto resolve(StockResolutionRequestDto request) {
        if (request == null || request.Code == null || request.Code.isBlank()
                || request.DetailList == null || request.DetailList.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una cantidad para resolver");
        }
        StockExitHeadEntity head = headRepository.findForUpdate(request.Code);
        requireResolvable(head);
        List<KardexEntity> totals = new ArrayList<>();
        List<KardexZoneEntity> zones = new ArrayList<>();

        for (StockResolutionLineDto line : request.DetailList) {
            StockExitDetEntity detail = detRepository.findForUpdate(request.Code, line.ItemNumber);
            if (detail == null || detail.OriginStockExitCod != null) {
                throw new IllegalArgumentException("No existe el item original " + line.ItemNumber);
            }
            int currentVersion = detail.ResolutionVersion == null ? 0 : detail.ResolutionVersion;
            int expectedVersion = line.ResolutionVersion == null ? 0 : line.ResolutionVersion;
            if (currentVersion != expectedVersion) {
                throw new IllegalStateException(
                        "El item " + line.ItemNumber + " ya fue resuelto por otro proceso. Recargue la pagina"
                );
            }
            int quantity = validation.positive(line.NumUnit, "La cantidad a resolver");
            if (quantity > detail.NumUnitPending) {
                throw new IllegalArgumentException("La cantidad excede el pendiente del item " + line.ItemNumber);
            }
            validation.validateResolution(
                    line.ResolutionType, line.ResolutionReasonCode, line.Observation, line.NextReviewDate
            );

            int newVersion = currentVersion + 1;
            String event = StockMovementConstants.EVENT_RESOLUTION_PREFIX + newVersion;
            boolean release = StockMovementConstants.RESOLUTION_RELEASE.equals(line.ResolutionType);
            boolean maintain = StockMovementConstants.RESOLUTION_MAINTAIN.equals(line.ResolutionType);

            if (release) {
                detail.NumUnitPending -= quantity;
                detail.NumUnitResolvedIn += quantity;
                detail.ResolvedInReasonCode = line.ResolutionReasonCode;
                zones.addAll(zone(detail, head, quantity, -quantity, event));
            } else if (!maintain) {
                detail.NumUnitPending -= quantity;
                detail.NumUnitResolvedOut += quantity;
                detail.ResolvedOutReasonCode = line.ResolutionReasonCode;
                detail.ResolvedOutType = line.ResolutionType;
                totals.add(total(detail, head, quantity));
                zones.addAll(zone(detail, head, 0, -quantity, event));
            }

            detail.ResolutionVersion = newVersion;
            detail.ResolutionType = line.ResolutionType;
            detail.ResolutionReasonCode = maintain ? null : line.ResolutionReasonCode;
            detail.Observation = line.Observation;
            detail.NextReviewDate = line.NextReviewDate;
            detail.addSessionModify(getUserCod());
            detRepository.save(detail);
        }

        kardexCreateService.saveAll(totals, zones);
        head.ResolutionUser = getUserCod();
        head.ResolutionDate = new Date();
        head.addSessionModify(getUserCod());
        headRepository.save(head);
        return findById(head.StockExitCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockExitRegisterDto changeStatus(StockMovementActionDto action, String status) {
        StockExitHeadEntity head = headRepository.findForUpdate(action.Code);
        requirePending(head);
        if (!StatusConst.REJECTED.equals(status) && !StatusConst.CANCELLED.equals(status)) {
            throw new IllegalArgumentException("Estado no soportado");
        }
        head.ProcessStatus = status;
        head.Observation = appendObservation(head.Observation, action.Observation);
        head.addSessionModify(getUserCod());
        headRepository.save(head);
        return findById(action.Code);
    }

    private void confirmOriginal(StockExitHeadEntity head, List<StockExitDetEntity> details,
                                 List<KardexEntity> totals, List<KardexZoneEntity> zones) {
        boolean direct = StockMovementConstants.MODE_DIRECT.equals(head.MovementMode);
        for (StockExitDetEntity d : details) {
            int qty = validation.positive(d.NumUnit, "La cantidad");
            d.NumUnitPending = direct ? 0 : qty;
            d.NumUnitResolvedIn = 0;
            d.NumUnitResolvedOut = direct ? qty : 0;
            if (direct) totals.add(total(d, head, qty));
            zones.addAll(zone(d, head, -qty, direct ? 0 : qty, StockMovementConstants.EVENT_CONFIRMATION));
            d.addSessionModify(getUserCod());
            detRepository.save(d);
        }
    }

    private void normalizeAndValidate(StockExitHeadEntity head, List<StockExitDetEntity> details) {
        if (details == null || details.isEmpty()) throw new IllegalArgumentException("Debe registrar al menos un producto");
        if (!StockMovementConstants.MODE_DIRECT.equals(head.MovementMode)
                && !StockMovementConstants.MODE_UNAVAILABLE.equals(head.MovementMode)) {
            throw new IllegalArgumentException("Seleccione el modo del movimiento");
        }
        validation.requireReason(9, head.ReasonCode, "El motivo de retiro");
        head.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        head.OriginStockExitCod = null;
        for (StockExitDetEntity d : details) {
            validation.positive(d.NumUnit, "La cantidad");
            if (d.ProductCod == null || d.ProductCod.isBlank() || d.Variant == null || d.WarehouseCod == null) {
                throw new IllegalArgumentException("Producto, variante y almacen son obligatorios");
            }
            d.ProductUnitFactor = d.ProductUnitFactor == null || d.ProductUnitFactor <= 0 ? 1 : d.ProductUnitFactor;
            d.ProductUnitName = clean(d.ProductUnitName).isEmpty() ? "UNIDAD" : d.ProductUnitName;
            d.LotNumber = clean(d.LotNumber);
            d.NumUnitPending = 0;
            d.NumUnitResolvedIn = 0;
            d.NumUnitResolvedOut = 0;
            d.ResolvedInReasonCode = null;
            d.ResolvedOutReasonCode = null;
            d.ResolvedOutType = null;
            d.ResolutionVersion = 0;
            d.OriginStockExitCod = null;
            d.OriginItemNumber = null;
            d.ResolutionType = null;
            d.ResolutionReasonCode = null;
            if (StockMovementConstants.MODE_UNAVAILABLE.equals(head.MovementMode)) {
                validation.requireReason(10, d.UnavailableReasonCode, "El motivo de no disponible");
            } else d.UnavailableReasonCode = null;
        }
    }

    private void requireResolvable(StockExitHeadEntity head) {
        if (head == null) throw new IllegalArgumentException("No existe el retiro de stock");
        requireStore(head.StoreCod);
        if (!StockMovementConstants.PROCESS_ORIGINAL.equals(head.ProcessType)
                || !StockMovementConstants.MODE_UNAVAILABLE.equals(head.MovementMode)
                || !StatusConst.CONFIRMED.equals(head.ProcessStatus)) {
            throw new IllegalStateException(
                    "Solo se puede resolver un retiro confirmado con stock no disponible"
            );
        }
    }

    private KardexEntity total(StockExitDetEntity d, StockExitHeadEntity h, int qty) {
        return KardexEntity.build(h.StockExitCod, d.ItemNumber, StockMovementConstants.SOURCE_EXIT,
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT,
                d.ProductCod, d.Variant, h.StoreCod, d.WarehouseCod, qty, d.LotNumber, d.ExpirationDate,
                1, getUserCod());
    }

    private List<KardexZoneEntity> zone(StockExitDetEntity d, StockExitHeadEntity h,
                                        int physical, int unavailable, String event) {
        return KardexZoneEntity.buildInventoryMovement(
                h.StockExitCod, d.ItemNumber, StockMovementConstants.SOURCE_EXIT, event,
                d.ProductCod, d.Variant, h.StoreCod, d.WarehouseCod, d.LotNumber, d.ExpirationDate,
                getUserCod(), physical, unavailable
        );
    }

    private void requirePending(StockExitHeadEntity head) {
        if (head == null) throw new IllegalArgumentException("No existe el retiro de stock");
        requireStore(head.StoreCod);
        if (!StatusConst.PENDING.equals(head.ProcessStatus)) {
            throw new IllegalStateException("El documento ya no se encuentra pendiente");
        }
    }

    private void requireStore(String storeCod) {
        if (!getStoreCod().equals(storeCod)) throw new IllegalArgumentException("El documento pertenece a otra tienda");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String appendObservation(String current, String added) {
        if (added == null || added.isBlank()) return current;
        return (current == null || current.isBlank()) ? added.trim() : current + "\n" + added.trim();
    }
}
