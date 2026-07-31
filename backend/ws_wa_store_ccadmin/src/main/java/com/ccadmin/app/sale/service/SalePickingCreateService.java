package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalePickingConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePickingLineDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SalePickingCreateService extends SessionService {

    private static final int MAX_LOT_LENGTH = 32;

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
    @Autowired
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;

    @Transactional(rollbackOn = Exception.class)
    public SaleDetailDto confirm(SalePickingConfirmDto request) throws SaleException {
        this.validateRequest(request);

        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(request.SaleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + request.SaleCod));
        if (!SaleConstants.PENDING.equals(saleHead.SaleStatus)) {
            throw new SaleException("Solo se puede confirmar el pickeo de una venta pendiente");
        }
        if ("S".equals(saleHead.IsPickingConfirmed)) {
            throw new SaleException("El pickeo de la venta ya fue confirmado");
        }

        List<SaleDetEntity> saleDetailList = this.saleDetRepository.findBySaleCod(request.SaleCod);
        List<SaleDetWarehouseEntity> currentWarehouseList =
                this.saleDetWarehouseRepository.findBySaleCodForUpdate(request.SaleCod);
        List<SaleDetWarehouseEntity> pickedWarehouseList = this.buildPickedWarehouseList(
                request, saleDetailList, currentWarehouseList
        );

        saleHead.IsPickingConfirmed = "S";
        saleHead.addSession(getUserCod());
        this.saleDetWarehouseRepository.saveAll(pickedWarehouseList);
        this.saleHeadRepository.save(saleHead);

        return this.saleSearchService.findById(saleHead.SaleCod);
    }

    private void validateRequest(SalePickingConfirmDto request) throws SaleException {
        if (request == null || request.SaleCod == null || request.SaleCod.isBlank()) {
            throw new SaleException("El codigo de venta es obligatorio para confirmar el pickeo");
        }
        if (request.DetailList == null || request.DetailList.isEmpty()) {
            throw new SaleException("Debe ingresar el pickeo de todos los productos");
        }
    }

    private List<SaleDetWarehouseEntity> buildPickedWarehouseList(
            SalePickingConfirmDto request,
            List<SaleDetEntity> saleDetailList,
            List<SaleDetWarehouseEntity> currentWarehouseList
    ) throws SaleException {
        if (saleDetailList.isEmpty()) {
            throw new SaleException("La venta no tiene productos para pickear");
        }

        Map<Integer, SaleDetEntity> saleDetailByItem = saleDetailList.stream()
                .collect(Collectors.toMap(item -> item.ItemNumber, item -> item, (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<Integer, List<SalePickingLineDto>> requestedByItem = request.DetailList.stream()
                .collect(Collectors.groupingBy(item -> item.ItemNumber, LinkedHashMap::new, Collectors.toList()));
        Map<Integer, List<SaleDetWarehouseEntity>> currentByItem = currentWarehouseList.stream()
                .collect(Collectors.groupingBy(item -> item.ItemNumber, LinkedHashMap::new, Collectors.toList()));

        for (Integer requestedItem : requestedByItem.keySet()) {
            if (!saleDetailByItem.containsKey(requestedItem)) {
                throw new SaleException("El item " + requestedItem + " no pertenece a la venta");
            }
        }

        List<SaleDetWarehouseEntity> result = new ArrayList<>();
        for (SaleDetEntity saleDetail : saleDetailList) {
            List<SalePickingLineDto> pickingLineList = requestedByItem.get(saleDetail.ItemNumber);
            if (pickingLineList == null || pickingLineList.isEmpty()) {
                throw new SaleException("Falta pickear el item " + saleDetail.ItemNumber);
            }

            List<SaleDetWarehouseEntity> currentItemWarehouseList = currentByItem.get(saleDetail.ItemNumber);
            if (currentItemWarehouseList == null || currentItemWarehouseList.size() != 1) {
                throw new SaleException(
                        "El item " + saleDetail.ItemNumber + " no tiene una asignacion unica de almacen"
                );
            }

            this.validatePickingLines(saleDetail, pickingLineList);
            SaleDetWarehouseEntity baseWarehouse = currentItemWarehouseList.get(0);
            for (int index = 0; index < pickingLineList.size(); index++) {
                SalePickingLineDto pickingLine = pickingLineList.get(index);
                SaleDetWarehouseEntity pickedWarehouse = index == 0
                        ? baseWarehouse
                        : new SaleDetWarehouseEntity();
                pickedWarehouse.SaleCod = saleDetail.SaleCod;
                pickedWarehouse.ItemNumber = saleDetail.ItemNumber;
                pickedWarehouse.AllocationNumber = index + 1;
                pickedWarehouse.ProductCod = saleDetail.ProductCod;
                pickedWarehouse.Variant = saleDetail.Variant;
                pickedWarehouse.WarehouseCod = baseWarehouse.WarehouseCod;
                pickedWarehouse.NumUnit = pickingLine.NumUnit;
                pickedWarehouse.ProductUnitName = saleDetail.ProductUnitName;
                pickedWarehouse.ProductUnitFactor = saleDetail.ProductUnitFactor;
                pickedWarehouse.LotNumber = pickingLine.LotNumber.trim();
                pickedWarehouse.ExpirationDate = pickingLine.ExpirationDate;
                pickedWarehouse.session(getUserCod());
                result.add(pickedWarehouse);
            }
        }
        return List.copyOf(result);
    }

    private void validatePickingLines(
            SaleDetEntity saleDetail,
            List<SalePickingLineDto> pickingLineList
    ) throws SaleException {
        int totalPicked = 0;
        Set<String> lotKeySet = new HashSet<>();
        int productUnitFactor = Math.max(saleDetail.ProductUnitFactor, 1);

        for (SalePickingLineDto pickingLine : pickingLineList) {
            if (pickingLine.NumUnit <= 0) {
                throw new SaleException("La cantidad pickeada debe ser mayor que cero");
            }
            this.productOperationConfigShared.validateInternalQuantity(
                    saleDetail.ProductCod, pickingLine.NumUnit, productUnitFactor
            );
            if (pickingLine.LotNumber == null || pickingLine.LotNumber.isBlank()) {
                throw new SaleException("El lote es obligatorio para el item " + saleDetail.ItemNumber);
            }
            String normalizedLot = pickingLine.LotNumber.trim();
            if (normalizedLot.length() > MAX_LOT_LENGTH) {
                throw new SaleException("El lote no puede superar " + MAX_LOT_LENGTH + " caracteres");
            }
            LocalDate expirationDate = pickingLine.ExpirationDate == null
                    ? null
                    : new Date(pickingLine.ExpirationDate.getTime()).toLocalDate();
            if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
                throw new SaleException("La fecha de vencimiento no puede estar vencida");
            }
            String expirationKey = expirationDate == null ? "" : expirationDate.toString();
            if (!lotKeySet.add(normalizedLot.toUpperCase(Locale.ROOT) + "|" + expirationKey)) {
                throw new SaleException("No repita el mismo lote y fecha para un producto");
            }
            try {
                totalPicked = Math.addExact(totalPicked, pickingLine.NumUnit);
            } catch (ArithmeticException ex) {
                throw new SaleException("La cantidad pickeada excede el limite permitido");
            }
        }

        if (totalPicked != saleDetail.NumUnit) {
            throw new SaleException(
                    "El item " + saleDetail.ItemNumber + " requiere " + saleDetail.NumUnit
                            + " unidades y se pickearon " + totalPicked
            );
        }
    }
}
