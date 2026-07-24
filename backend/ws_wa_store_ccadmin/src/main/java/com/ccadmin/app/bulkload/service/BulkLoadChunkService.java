package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDestinationRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
import com.ccadmin.app.inventory.service.StockEntryCreateService;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceLineDto;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceResultDto;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceUpdateDto;
import com.ccadmin.app.product.service.ProductConfigCreateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BulkLoadChunkService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadDestinationRepository destinationRepository;
    private final BulkLoadDetRepository detRepository;
    private final ProductConfigCreateService productConfigCreateService;
    private final StockEntryCreateService stockEntryCreateService;

    public BulkLoadChunkService(BulkLoadHeadRepository headRepository,
                                BulkLoadDestinationRepository destinationRepository,
                                BulkLoadDetRepository detRepository,
                                ProductConfigCreateService productConfigCreateService,
                                StockEntryCreateService stockEntryCreateService) {
        this.headRepository = headRepository;
        this.destinationRepository = destinationRepository;
        this.detRepository = detRepository;
        this.productConfigCreateService = productConfigCreateService;
        this.stockEntryCreateService = stockEntryCreateService;
    }

    /**
     * Prepara recursos particulares antes de abrir la transaccion del bloque.
     * Para stock solicita get_cod_trx, cuyo correlativo se confirma de inmediato.
     */
    public String prepareNextChunk(String code) {
        BulkLoadHeadEntity head = headRepository.findById(code).orElse(null);
        if (head == null || !BulkLoadConstants.TYPE_STOCK_ENTRY.equals(head.BulkLoadType)) {
            return null;
        }
        String storeCod = detRepository.findNextPendingStore(code);
        return storeCod == null || storeCod.isBlank()
                ? null : stockEntryCreateService.createCode(storeCod);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.QUEUED.equals(head.ProcessStatus)) return false;
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.WORKING;
        head.StartDate = head.StartDate == null ? now : head.StartDate;
        head.LastHeartbeatDate = now;
        head.AttemptCount = value(head.AttemptCount) + 1;
        head.StatusMessage = "Procesando en segundo plano";
        head.addSessionModify(processUser(head));
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            destination.ProcessStatus = BulkLoadConstants.WORKING;
            destination.StartDate = destination.StartDate == null ? now : destination.StartDate;
            destination.StatusMessage = "Procesando";
            destination.addSessionModify(processUser(head));
            destinationRepository.save(destination);
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int processNextChunk(String code, String stockEntryCod) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.WORKING.equals(head.ProcessStatus)) return 0;
        List<BulkLoadDetEntity> detailList = detRepository.findNextPendingForUpdate(code);
        if (detailList.isEmpty()) return 0;
        String userCod = processUser(head);
        Date now = new Date();
        detailList.forEach(detail -> {
            detail.ProcessStatus = BulkLoadConstants.WORKING;
            detail.StartDate = detail.StartDate == null ? now : detail.StartDate;
            detail.AttemptCount = value(detail.AttemptCount) + 1;
            detail.addSessionModify(userCod);
        });
        detRepository.saveAll(detailList);

        if (BulkLoadConstants.TYPE_PRODUCT_PRICE.equals(head.BulkLoadType)) {
            processPrice(detailList, userCod);
        } else if (BulkLoadConstants.TYPE_STOCK_ENTRY.equals(head.BulkLoadType)) {
            if (stockEntryCod == null || stockEntryCod.isBlank()) {
                throw new IllegalStateException(
                        "No se genero el codigo de entrada para el bloque de stock"
                );
            }
            processStock(head, detailList, userCod, stockEntryCod);
        } else {
            throw new IllegalStateException("Tipo de carga no soportado");
        }

        Date end = new Date();
        detailList.forEach(detail -> {
            detail.ProcessStatus = BulkLoadConstants.CONFIRMED;
            detail.EndDate = end;
            detail.addSessionModify(userCod);
        });
        detRepository.saveAll(detailList);
        detRepository.flush();
        updateCounters(head, userCod, false);
        return detailList.size();
    }

    private void processPrice(List<BulkLoadDetEntity> detailList, String userCod) {
        ProductConfigBulkPriceUpdateDto request = new ProductConfigBulkPriceUpdateDto();
        for (BulkLoadDetEntity detail : detailList) {
            ProductConfigBulkPriceLineDto line = new ProductConfigBulkPriceLineDto();
            line.ReferenceItemNumber = detail.ItemNumber;
            line.ProductCod = text(detail.Payload.get("ProductCod"));
            line.StoreCod = text(detail.Payload.get("StoreCod"));
            line.NumPrice = decimal(detail.Payload.get("NumPrice")).setScale(2);
            request.DetailList.add(line);
        }
        List<ProductConfigBulkPriceResultDto> resultList =
                productConfigCreateService.saveBulkPrices(request, userCod);
        Map<Integer, ProductConfigBulkPriceResultDto> resultMap = new HashMap<>();
        resultList.forEach(result -> resultMap.put(result.ReferenceItemNumber, result));
        for (BulkLoadDetEntity detail : detailList) {
            ProductConfigBulkPriceResultDto businessResult = resultMap.get(detail.ItemNumber);
            if (businessResult == null) {
                throw new IllegalStateException(
                        "No se obtuvo resultado para el item " + detail.ItemNumber
                );
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("OldPrice", businessResult.OldPrice);
            result.put("NewPrice", businessResult.NewPrice);
            result.put("Changed", businessResult.Changed);
            detail.ResultData = result;
        }
    }

    private void processStock(BulkLoadHeadEntity head, List<BulkLoadDetEntity> detailList,
                              String userCod, String stockEntryCod) {
        String storeCod = detailList.getFirst().StoreCod;
        if (detailList.stream().anyMatch(item -> !Objects.equals(storeCod, item.StoreCod))) {
            throw new IllegalStateException("Un bloque de stock no puede mezclar locales");
        }
        StockEntryBulkCreateDto request = new StockEntryBulkCreateDto();
        request.StockEntryCod = stockEntryCod;
        request.StoreCod = storeCod;
        request.BulkLoadCod = head.BulkLoadCod;
        for (BulkLoadDetEntity detail : detailList) {
            StockEntryBulkLineDto line = new StockEntryBulkLineDto();
            line.ReferenceItemNumber = detail.ItemNumber;
            line.SourceRowNumber = detail.SourceRowNumber;
            line.ProductCod = text(detail.Payload.get("ProductCod"));
            line.Variant = text(detail.Payload.get("Variant"));
            line.WarehouseCod = text(detail.Payload.get("WarehouseCod"));
            line.ProductUnitName = defaultText(
                    detail.Payload.get("ProductUnitName"), "NIU"
            );
            line.ProductUnitFactor = integer(detail.Payload.get("ProductUnitFactor"));
            line.NumUnit = integer(detail.Payload.get("NumPhysicalStock"));
            request.DetailList.add(line);
        }
        StockEntryBulkResultDto businessResult =
                stockEntryCreateService.createAndConfirmBulk(request, userCod);
        for (BulkLoadDetEntity detail : detailList) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("StockEntryCod", businessResult.StockEntryCod);
            result.put("ItemNumber",
                    businessResult.ItemNumberByReference.get(detail.ItemNumber));
            detail.ResultData = result;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void finish(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.WORKING.equals(head.ProcessStatus)) return;
        String userCod = processUser(head);
        updateCounters(head, userCod, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void fail(String code, Exception exception) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || BulkLoadConstants.FINALIZED.equals(head.ProcessStatus)
                || BulkLoadConstants.CANCELLED.equals(head.ProcessStatus)) {
            return;
        }
        String userCod = processUser(head);
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.ERROR;
        head.EndDate = now;
        head.LastHeartbeatDate = now;
        head.StatusMessage = trimMessage(exception == null ? null : exception.getMessage());
        head.addSessionModify(userCod);
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            if (!BulkLoadConstants.FINALIZED.equals(destination.ProcessStatus)) {
                destination.ProcessStatus = BulkLoadConstants.ERROR;
                destination.EndDate = now;
                destination.StatusMessage = head.StatusMessage;
                destination.addSessionModify(userCod);
                destinationRepository.save(destination);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recover(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || (!BulkLoadConstants.WORKING.equals(head.ProcessStatus)
                && !BulkLoadConstants.QUEUED.equals(head.ProcessStatus))) {
            return;
        }
        head.ProcessStatus = BulkLoadConstants.QUEUED;
        head.StatusMessage = "Recuperado al iniciar la aplicacion";
        head.LastHeartbeatDate = new Date();
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            if (!BulkLoadConstants.FINALIZED.equals(destination.ProcessStatus)) {
                destination.ProcessStatus = BulkLoadConstants.QUEUED;
                destination.StatusMessage = "En cola";
                destinationRepository.save(destination);
            }
        }
    }

    private void updateCounters(BulkLoadHeadEntity head, String userCod, boolean finish) {
        int success = detRepository.countByProcessStatus(
                head.BulkLoadCod, BulkLoadConstants.CONFIRMED
        );
        int errors = detRepository.countByProcessStatus(
                head.BulkLoadCod, BulkLoadConstants.ERROR
        );
        int processed = success + errors;
        int total = value(head.NumTotalDetails);
        head.NumSuccessDetails = success;
        head.NumErrorDetails = errors;
        head.NumProcessedDetails = processed;
        head.ProgressPercent = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(processed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        head.LastHeartbeatDate = new Date();
        head.StatusMessage = processed + " de " + total + " registros procesados";

        for (BulkLoadDestinationEntity destination
                : destinationRepository.findByCode(head.BulkLoadCod)) {
            int storeSuccess = detRepository.countByStoreAndProcessStatus(
                    head.BulkLoadCod, destination.StoreCod, BulkLoadConstants.CONFIRMED
            );
            int storeErrors = detRepository.countByStoreAndProcessStatus(
                    head.BulkLoadCod, destination.StoreCod, BulkLoadConstants.ERROR
            );
            destination.NumSuccessDetails = storeSuccess;
            destination.NumErrorDetails = storeErrors;
            destination.NumProcessedDetails = storeSuccess + storeErrors;
            destination.StatusMessage = destination.NumProcessedDetails
                    + " de " + value(destination.NumTotalDetails) + " registros procesados";
            if (finish) {
                destination.ProcessStatus = BulkLoadConstants.FINALIZED;
                destination.EndDate = new Date();
            }
            destination.addSessionModify(userCod);
            destinationRepository.save(destination);
        }

        if (finish) {
            if (processed != total) {
                throw new IllegalStateException(
                        "No se puede finalizar: existen registros pendientes"
                );
            }
            head.ProcessStatus = BulkLoadConstants.FINALIZED;
            head.ProgressPercent = BigDecimal.valueOf(100).setScale(2);
            head.EndDate = new Date();
            head.StatusMessage = errors == 0
                    ? "Carga finalizada correctamente"
                    : "Carga finalizada con errores";
        }
        head.addSessionModify(userCod);
        headRepository.save(head);
    }

    private String processUser(BulkLoadHeadEntity head) {
        return head.CreationUser == null || head.CreationUser.isBlank()
                ? "SISTEMA" : head.CreationUser;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String defaultText(Object value, String defaultValue) {
        String text = text(value);
        return text.isBlank() ? defaultValue : text;
    }

    private int integer(Object value) {
        return decimal(value).intValueExact();
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        return new BigDecimal(text(value));
    }

    private String trimMessage(String message) {
        String result = message == null || message.isBlank()
                ? "Error inesperado durante el procesamiento" : message.trim();
        return result.length() <= 512 ? result : result.substring(0, 512);
    }
}
