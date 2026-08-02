package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailSplitLineDto;
import com.ccadmin.app.sale.model.dto.SalePickingConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePickingLineDto;
import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDetTaxRepository;
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
    private SaleDetTaxRepository saleDetTaxRepository;
    @Autowired
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private SaleTaxCalculationService saleTaxCalculationService;
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
        List<SaleDetTaxEntity> saleDetailTaxList = this.saleDetTaxRepository.findBySaleCod(request.SaleCod);
        List<SaleDetWarehouseEntity> currentWarehouseList =
                this.saleDetWarehouseRepository.findBySaleCodForUpdate(request.SaleCod);
        PickingResult pickingResult = this.buildPickingResult(
                request, saleDetailList, saleDetailTaxList, currentWarehouseList
        );

        saleHead.IsPickingConfirmed = "S";
        saleHead.addSession(getUserCod());
        this.saleDetRepository.saveAll(pickingResult.saleDetailList());
        this.saleDetWarehouseRepository.saveAll(pickingResult.warehouseList());
        this.saleDetTaxRepository.saveAll(pickingResult.taxList());
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

    private PickingResult buildPickingResult(
            SalePickingConfirmDto request,
            List<SaleDetEntity> saleDetailList,
            List<SaleDetTaxEntity> saleDetailTaxList,
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
        Map<Integer, List<SaleDetTaxEntity>> taxByItem = saleDetailTaxList.stream()
                .collect(Collectors.groupingBy(item -> item.ItemNumber, LinkedHashMap::new, Collectors.toList()));

        for (Integer requestedItem : requestedByItem.keySet()) {
            if (!saleDetailByItem.containsKey(requestedItem)) {
                throw new SaleException("El item " + requestedItem + " no pertenece a la venta");
            }
        }

        int nextItemNumber = saleDetailList.stream()
                .mapToInt(item -> item.ItemNumber)
                .max()
                .orElse(0) + 1;
        List<SaleDetEntity> pickedDetailList = new ArrayList<>();
        List<SaleDetWarehouseEntity> pickedWarehouseList = new ArrayList<>();
        List<SaleDetTaxEntity> pickedTaxList = new ArrayList<>();
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
            List<SaleDetailSplitLineDto> splitLineList = new ArrayList<>();
            for (int index = 0; index < pickingLineList.size(); index++) {
                SalePickingLineDto pickingLine = pickingLineList.get(index);
                int itemNumber = index == 0 ? saleDetail.ItemNumber : nextItemNumber++;
                splitLineList.add(new SaleDetailSplitLineDto(
                        itemNumber,
                        pickingLine.NumUnit,
                        this.normalizeLotNumber(pickingLine.LotNumber),
                        pickingLine.ExpirationDate
                ));
            }
            SaleTaxCalculationResultDto splitResult = this.saleTaxCalculationService.splitExistingSaleDetail(
                    saleDetail,
                    taxByItem.getOrDefault(saleDetail.ItemNumber, List.of()),
                    splitLineList,
                    getUserCod()
            );
            pickedDetailList.addAll(splitResult.DetailList);
            pickedTaxList.addAll(splitResult.TaxDetailList);

            for (int index = 0; index < splitLineList.size(); index++) {
                SaleDetailSplitLineDto splitLine = splitLineList.get(index);
                SaleDetEntity pickedDetail = splitResult.DetailList.get(index);
                SaleDetWarehouseEntity pickedWarehouse = index == 0
                        ? baseWarehouse
                        : this.copyWarehouse(baseWarehouse, splitLine.ItemNumber);
                pickedWarehouse.SaleCod = saleDetail.SaleCod;
                pickedWarehouse.ItemNumber = splitLine.ItemNumber;
                pickedWarehouse.ProductCod = pickedDetail.ProductCod;
                pickedWarehouse.Variant = pickedDetail.Variant;
                pickedWarehouse.WarehouseCod = baseWarehouse.WarehouseCod;
                pickedWarehouse.NumUnit = splitLine.NumUnit;
                pickedWarehouse.ProductUnitName = pickedDetail.ProductUnitName;
                pickedWarehouse.ProductUnitFactor = pickedDetail.ProductUnitFactor;
                pickedWarehouse.LotNumber = splitLine.LotNumber;
                pickedWarehouse.ExpirationDate = splitLine.ExpirationDate;
                pickedWarehouse.session(getUserCod()).validate();
                pickedWarehouseList.add(pickedWarehouse);
            }
        }
        return new PickingResult(
                List.copyOf(pickedDetailList),
                List.copyOf(pickedWarehouseList),
                List.copyOf(pickedTaxList)
        );
    }

    private SaleDetWarehouseEntity copyWarehouse(SaleDetWarehouseEntity source, int itemNumber) {
        SaleDetWarehouseEntity target = new SaleDetWarehouseEntity();
        target.SaleCod = source.SaleCod;
        target.ItemNumber = itemNumber;
        target.ProductCod = source.ProductCod;
        target.Variant = source.Variant;
        target.WarehouseCod = source.WarehouseCod;
        target.ProductUnitName = source.ProductUnitName;
        target.ProductUnitFactor = source.ProductUnitFactor;
        return target;
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
            String normalizedLot = this.normalizeLotNumber(pickingLine.LotNumber);
            if (normalizedLot != null && normalizedLot.length() > MAX_LOT_LENGTH) {
                throw new SaleException("El lote no puede superar " + MAX_LOT_LENGTH + " caracteres");
            }
            LocalDate expirationDate = pickingLine.ExpirationDate == null
                    ? null
                    : new Date(pickingLine.ExpirationDate.getTime()).toLocalDate();
            if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
                throw new SaleException("La fecha de vencimiento no puede estar vencida");
            }
            String expirationKey = expirationDate == null ? "" : expirationDate.toString();
            String lotKey = normalizedLot == null ? "" : normalizedLot.toUpperCase(Locale.ROOT);
            if (!lotKeySet.add(lotKey + "|" + expirationKey)) {
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

    private String normalizeLotNumber(String lotNumber) {
        if (lotNumber == null || lotNumber.isBlank()) {
            return null;
        }
        return lotNumber.trim();
    }

    private record PickingResult(
            List<SaleDetEntity> saleDetailList,
            List<SaleDetWarehouseEntity> warehouseList,
            List<SaleDetTaxEntity> taxList
    ) {
    }
}
