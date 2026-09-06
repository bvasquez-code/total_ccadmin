package com.ccadmin.app.producttraceability.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryRegisterDto;
import com.ccadmin.app.inventory.model.dto.StockExitRegisterDto;
import com.ccadmin.app.inventory.service.StockEntrySearchService;
import com.ccadmin.app.inventory.service.StockExitService;
import com.ccadmin.app.producttraceability.model.dto.ProductTraceabilityOperationDto;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.pucharse.model.dto.PucharseDetailsDto;
import com.ccadmin.app.pucharse.service.PucharseService;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.service.CreditNoteSearchService;
import com.ccadmin.app.sale.service.SaleSearchService;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferDetailDto;
import com.ccadmin.app.transfer.model.dto.TransferRequestDetailDto;
import com.ccadmin.app.transfer.service.TransferRequestSearchService;
import com.ccadmin.app.transfer.service.TransferSearchService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductTraceabilityTaskService {

    private final ProductTraceabilityCreateService productTraceabilityCreateService;
    private final ProductTraceabilityTechnicalLotCreateService
            productTraceabilityTechnicalLotCreateService;
    private final PucharseService pucharseService;
    private final SaleSearchService saleSearchService;
    private final CreditNoteSearchService creditNoteSearchService;
    private final TransferSearchService transferSearchService;
    private final TransferRequestSearchService transferRequestSearchService;
    private final StockEntrySearchService stockEntrySearchService;
    private final StockExitService stockExitService;

    public ProductTraceabilityTaskService(
            ProductTraceabilityCreateService productTraceabilityCreateService,
            ProductTraceabilityTechnicalLotCreateService
                    productTraceabilityTechnicalLotCreateService,
            PucharseService pucharseService,
            SaleSearchService saleSearchService,
            CreditNoteSearchService creditNoteSearchService,
            TransferSearchService transferSearchService,
            TransferRequestSearchService transferRequestSearchService,
            StockEntrySearchService stockEntrySearchService,
            StockExitService stockExitService
    ) {
        this.productTraceabilityCreateService = productTraceabilityCreateService;
        this.productTraceabilityTechnicalLotCreateService =
                productTraceabilityTechnicalLotCreateService;
        this.pucharseService = pucharseService;
        this.saleSearchService = saleSearchService;
        this.creditNoteSearchService = creditNoteSearchService;
        this.transferSearchService = transferSearchService;
        this.transferRequestSearchService = transferRequestSearchService;
        this.stockEntrySearchService = stockEntrySearchService;
        this.stockExitService = stockExitService;
    }

    public void processPurchase(String operationCode, String storeCode) {
        PucharseDetailsDto document = this.pucharseService.findById(operationCode);
        this.requireStore(document.Headboard.StoreCod, storeCode, operationCode);
        Map<Integer, BigDecimal> unitCostByItem = document.DetailList.stream()
                .collect(Collectors.toMap(
                        item -> item.ItemNumber,
                        item -> valueOrZero(item.NumUnitPrice)
                ));
        this.create(PucharseConstants.KARDEX_ZONE_SOURCE, operationCode, storeCode,
                null, unitCostByItem, Map.of());
    }

    public void processSale(String operationCode, String storeCode) {
        SaleDetailDto document = this.saleSearchService.findById(operationCode);
        this.requireStore(document.Headboard.StoreCod, storeCode, operationCode);
        Map<Integer, BigDecimal> unitSalePriceByItem = document.DetailList.stream()
                .collect(Collectors.toMap(
                        item -> item.ItemNumber,
                        item -> valueOrZero(item.NumUnitPriceSale)
                ));
        this.create(SaleConstants.KARDEX_ZONE_SOURCE_SALE, operationCode, storeCode,
                null, Map.of(), unitSalePriceByItem);
    }

    public void processCreditNote(String operationCode, String storeCode) {
        CreditNoteDetailDto document = this.creditNoteSearchService.findById(operationCode);
        if (document == null || document.Headboard == null) {
            throw new IllegalArgumentException("No existe la nota de credito " + operationCode);
        }
        this.requireStore(document.Headboard.StoreCod, storeCode, operationCode);
        Map<Integer, BigDecimal> unitSalePriceByItem = document.DetailList.stream()
                .map(item -> item.CreditNoteDet)
                .collect(Collectors.toMap(
                        item -> item.ItemNumber,
                        item -> valueOrZero(item.NumUnitPriceSale)
                ));
        this.create(SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                operationCode, storeCode, document.Headboard.SaleCod,
                Map.of(), unitSalePriceByItem);
    }

    public void processTransfer(String operationCode, String storeCode) {
        TransferDetailDto document = this.transferSearchService.findByTransferCod(operationCode);
        boolean validStore = document.transferHeadTe != null
                && (storeCode.equals(document.transferHeadTe.StoreCodOrigin)
                || storeCode.equals(document.transferHeadTe.StoreCodDest));
        validStore = validStore || document.transferHeadTs != null
                && (storeCode.equals(document.transferHeadTs.StoreCodOrigin)
                || storeCode.equals(document.transferHeadTs.StoreCodDest));
        this.requireStore(validStore, operationCode);
        this.create(TransferConstants.KARDEX_SOURCE_TABLE, operationCode, storeCode,
                null, Map.of(), Map.of());
    }

    public void processTransferRequest(String operationCode, String storeCode) {
        TransferRequestDetailDto document =
                this.transferRequestSearchService.findByTransferCod(operationCode);
        boolean validStore = document.transferHeadRequest != null
                && (storeCode.equals(document.transferHeadRequest.StoreCodOrigin)
                || storeCode.equals(document.transferHeadRequest.StoreCodDest));
        validStore = validStore || document.transferHead != null
                && (storeCode.equals(document.transferHead.StoreCodOrigin)
                || storeCode.equals(document.transferHead.StoreCodDest));
        this.requireStore(validStore, operationCode);
        this.create(TransferConstants.KARDEX_ZONE_SOURCE_REQUEST,
                operationCode, storeCode, null, Map.of(), Map.of());
    }

    public void processStockEntry(String operationCode, String storeCode) {
        StockEntryRegisterDto document =
                this.stockEntrySearchService.findById(operationCode, storeCode);
        Map<Integer, BigDecimal> unitCostByItem = document.DetailList.stream()
                .collect(Collectors.toMap(
                        item -> item.ItemNumber,
                        item -> valueOrZero(item.NumUnitPrice)
                ));
        this.create(StockMovementConstants.SOURCE_ENTRY, operationCode, storeCode,
                null, unitCostByItem, Map.of());
    }

    public void processStockExit(String operationCode, String storeCode) {
        StockExitRegisterDto document = this.stockExitService.findById(operationCode, storeCode);
        this.requireStore(document.Head.StoreCod, storeCode, operationCode);
        this.create(StockMovementConstants.SOURCE_EXIT, operationCode, storeCode,
                null, Map.of(), Map.of());
    }

    private void create(
            String sourceTable,
            String operationCode,
            String storeCode,
            String relatedOperationCode,
            Map<Integer, BigDecimal> unitCostByItem,
            Map<Integer, BigDecimal> unitSalePriceByItem
    ) {
        ProductTraceabilityOperationDto operation = new ProductTraceabilityOperationDto(
                sourceTable,
                operationCode,
                storeCode,
                relatedOperationCode,
                unitCostByItem,
                unitSalePriceByItem
        );
        this.productTraceabilityCreateService.create(
                this.productTraceabilityTechnicalLotCreateService
                        .reserveTechnicalLots(operation)
        );
    }

    private void requireStore(
            String documentStoreCode,
            String requestedStoreCode,
            String operationCode
    ) {
        this.requireStore(requestedStoreCode != null
                && requestedStoreCode.equals(documentStoreCode), operationCode);
    }

    private void requireStore(boolean validStore, String operationCode) {
        if (!validStore) {
            throw new IllegalArgumentException(
                    "La operacion " + operationCode + " no pertenece al local indicado"
            );
        }
    }

    private static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
