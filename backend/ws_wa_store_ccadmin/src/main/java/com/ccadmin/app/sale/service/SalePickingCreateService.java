package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalePickingConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePickingLineDto;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            int originalQuantity = saleDetail.NumUnit;
            SaleDetWarehouseEntity baseWarehouse = currentItemWarehouseList.get(0);
            List<Integer> itemNumberList = new ArrayList<>();
            List<BigDecimal> discountList = this.splitAmount(
                    saleDetail.NumDiscount, pickingLineList, originalQuantity, 2
            );
            List<BigDecimal> totalPriceList = this.splitAmount(
                    saleDetail.NumTotalPrice, pickingLineList, originalQuantity, 2
            );
            List<BigDecimal> subtotalList = this.splitAmount(
                    saleDetail.NumPriceSubTotal, pickingLineList, originalQuantity, 2
            );
            List<BigDecimal> totalTaxList = this.splitAmount(
                    saleDetail.NumTotalTax, pickingLineList, originalQuantity, 2
            );

            for (int index = 0; index < pickingLineList.size(); index++) {
                SalePickingLineDto pickingLine = pickingLineList.get(index);
                int itemNumber = index == 0 ? saleDetail.ItemNumber : nextItemNumber++;
                itemNumberList.add(itemNumber);

                SaleDetEntity pickedDetail = index == 0
                        ? saleDetail
                        : this.copySaleDetail(saleDetail, itemNumber);
                pickedDetail.ItemNumber = itemNumber;
                pickedDetail.NumUnit = pickingLine.NumUnit;
                pickedDetail.NumDiscount = discountList.get(index);
                pickedDetail.NumTotalPrice = totalPriceList.get(index);
                pickedDetail.NumPriceSubTotal = subtotalList.get(index);
                pickedDetail.NumTotalTax = totalTaxList.get(index);
                pickedDetail.LotNumber = this.normalizeLotNumber(pickingLine.LotNumber);
                pickedDetail.ExpirationDate = pickingLine.ExpirationDate;
                pickedDetail.session(getUserCod()).validate();
                pickedDetailList.add(pickedDetail);

                SaleDetWarehouseEntity pickedWarehouse = index == 0
                        ? baseWarehouse
                        : this.copyWarehouse(baseWarehouse, itemNumber);
                pickedWarehouse.SaleCod = saleDetail.SaleCod;
                pickedWarehouse.ItemNumber = itemNumber;
                pickedWarehouse.ProductCod = saleDetail.ProductCod;
                pickedWarehouse.Variant = saleDetail.Variant;
                pickedWarehouse.WarehouseCod = baseWarehouse.WarehouseCod;
                pickedWarehouse.NumUnit = pickingLine.NumUnit;
                pickedWarehouse.ProductUnitName = saleDetail.ProductUnitName;
                pickedWarehouse.ProductUnitFactor = saleDetail.ProductUnitFactor;
                pickedWarehouse.LotNumber = this.normalizeLotNumber(pickingLine.LotNumber);
                pickedWarehouse.ExpirationDate = pickingLine.ExpirationDate;
                pickedWarehouse.session(getUserCod()).validate();
                pickedWarehouseList.add(pickedWarehouse);
            }

            for (SaleDetTaxEntity saleDetailTax : taxByItem.getOrDefault(saleDetail.ItemNumber, List.of())) {
                List<BigDecimal> baseAmountList = this.splitAmount(
                        saleDetailTax.TaxBaseAmount, pickingLineList, originalQuantity, 2
                );
                List<BigDecimal> taxQuantityList = this.splitAmount(
                        saleDetailTax.TaxQuantity, pickingLineList, originalQuantity, 4
                );
                List<BigDecimal> taxAmountList = this.splitAmount(
                        saleDetailTax.TaxAmount, pickingLineList, originalQuantity, 2
                );
                for (int index = 0; index < pickingLineList.size(); index++) {
                    SaleDetTaxEntity pickedTax = index == 0
                            ? saleDetailTax
                            : this.copySaleDetailTax(saleDetailTax, itemNumberList.get(index));
                    pickedTax.ItemNumber = itemNumberList.get(index);
                    pickedTax.TaxBaseAmount = baseAmountList.get(index);
                    pickedTax.TaxQuantity = taxQuantityList.get(index);
                    pickedTax.TaxAmount = taxAmountList.get(index);
                    pickedTax.session(getUserCod()).validate();
                    pickedTaxList.add(pickedTax);
                }
            }
        }
        return new PickingResult(
                List.copyOf(pickedDetailList),
                List.copyOf(pickedWarehouseList),
                List.copyOf(pickedTaxList)
        );
    }

    private List<BigDecimal> splitAmount(
            BigDecimal originalAmount,
            List<SalePickingLineDto> pickingLineList,
            int originalQuantity,
            int scale
    ) {
        BigDecimal totalAmount = this.amount(originalAmount).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal assignedAmount = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        List<BigDecimal> result = new ArrayList<>();
        for (int index = 0; index < pickingLineList.size(); index++) {
            BigDecimal lineAmount;
            if (index == pickingLineList.size() - 1) {
                lineAmount = totalAmount.subtract(assignedAmount).setScale(scale, RoundingMode.HALF_UP);
            } else {
                lineAmount = totalAmount
                        .multiply(BigDecimal.valueOf(pickingLineList.get(index).NumUnit))
                        .divide(BigDecimal.valueOf(originalQuantity), scale, RoundingMode.HALF_UP);
                assignedAmount = assignedAmount.add(lineAmount).setScale(scale, RoundingMode.HALF_UP);
            }
            result.add(lineAmount);
        }
        return result;
    }

    private SaleDetEntity copySaleDetail(SaleDetEntity source, int itemNumber) {
        SaleDetEntity target = new SaleDetEntity();
        target.SaleCod = source.SaleCod;
        target.ItemNumber = itemNumber;
        target.ProductCod = source.ProductCod;
        target.Variant = source.Variant;
        target.NumUnitPrice = source.NumUnitPrice;
        target.NumUnitPriceSale = source.NumUnitPriceSale;
        target.ProductUnitName = source.ProductUnitName;
        target.ProductUnitFactor = source.ProductUnitFactor;
        target.IsAppliedTax = source.IsAppliedTax;
        return target;
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

    private SaleDetTaxEntity copySaleDetailTax(SaleDetTaxEntity source, int itemNumber) {
        SaleDetTaxEntity target = new SaleDetTaxEntity();
        target.SaleCod = source.SaleCod;
        target.ItemNumber = itemNumber;
        target.TaxLineNumber = source.TaxLineNumber;
        target.TaxCod = source.TaxCod;
        target.SunatTaxCod = source.SunatTaxCod;
        target.TaxName = source.TaxName;
        target.TaxAffectationCod = source.TaxAffectationCod;
        target.TaxAffectationName = source.TaxAffectationName;
        target.TaxCalculationType = source.TaxCalculationType;
        target.IsInformative = source.IsInformative;
        target.TaxRateValue = source.TaxRateValue;
        target.FixedUnitAmount = source.FixedUnitAmount;
        target.CalculationOrder = source.CalculationOrder;
        return target;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
