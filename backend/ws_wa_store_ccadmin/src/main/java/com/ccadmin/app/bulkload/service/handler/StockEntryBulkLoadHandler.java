package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
import com.ccadmin.app.inventory.service.StockEntryCreateService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StockEntryBulkLoadHandler implements BulkLoadTypeHandler {
    private final BulkLoadDetRepository bulkLoadDetRepository;
    private final StockEntryCreateService stockEntryCreateService;

    public StockEntryBulkLoadHandler(
            BulkLoadDetRepository bulkLoadDetRepository,
            StockEntryCreateService stockEntryCreateService
    ) {
        this.bulkLoadDetRepository = bulkLoadDetRepository;
        this.stockEntryCreateService = stockEntryCreateService;
    }

    @Override
    public String type() {
        return BulkLoadConstants.TYPE_STOCK_ENTRY;
    }

    @Override
    public BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        return stockEntryCreateService.prepareBulkStockLoad(request);
    }

    @Override
    public String prepareResource(String bulkLoadCod) {
        String storeCod =
                bulkLoadDetRepository.findNextPendingStore(bulkLoadCod);
        return storeCod == null || storeCod.isBlank()
                ? null : stockEntryCreateService.createCode(storeCod);
    }

    @Override
    public void execute(BulkLoadHeadEntity head,
                        List<BulkLoadDetEntity> detailList,
                        String userCod,
                        String resourceCode) {
        if (resourceCode == null || resourceCode.isBlank()) {
            throw new IllegalStateException(
                    "No se genero el codigo de entrada para el bloque de stock"
            );
        }
        String storeCod = detailList.getFirst().StoreCod;
        if (detailList.stream().anyMatch(
                item -> !Objects.equals(storeCod, item.StoreCod)
        )) {
            throw new IllegalStateException(
                    "Un bloque de stock no puede mezclar locales"
            );
        }

        StockEntryBulkCreateDto request = new StockEntryBulkCreateDto();
        request.StockEntryCod = resourceCode;
        request.StoreCod = storeCod;
        request.BulkLoadCod = head.BulkLoadCod;
        for (BulkLoadDetEntity detail : detailList) {
            StockEntryBulkLineDto line = new StockEntryBulkLineDto();
            line.ReferenceItemNumber = detail.ItemNumber;
            line.SourceRowNumber = detail.SourceRowNumber;
            line.ProductCod = BulkLoadHandlerSupport.text(
                    detail.Payload.get("ProductCod")
            );
            line.Variant = BulkLoadHandlerSupport.text(
                    detail.Payload.get("Variant")
            );
            line.WarehouseCod = BulkLoadHandlerSupport.text(
                    detail.Payload.get("WarehouseCod")
            );
            String unitName = BulkLoadHandlerSupport.text(
                    detail.Payload.get("ProductUnitName")
            );
            line.ProductUnitName = unitName.isBlank() ? "NIU" : unitName;
            line.ProductUnitFactor = BulkLoadHandlerSupport.integer(
                    detail.Payload.get("ProductUnitFactor")
            );
            line.NumUnit = BulkLoadHandlerSupport.integer(
                    detail.Payload.get("NumPhysicalStock")
            );
            request.DetailList.add(line);
        }

        StockEntryBulkResultDto businessResult =
                stockEntryCreateService.createAndConfirmBulk(request, userCod);
        for (BulkLoadDetEntity detail : detailList) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("StockEntryCod", businessResult.StockEntryCod);
            result.put(
                    "ItemNumber",
                    businessResult.ItemNumberByReference.get(detail.ItemNumber)
            );
            detail.ResultData = result;
        }
    }
}
