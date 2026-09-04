package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;

import java.util.ArrayList;
import java.util.List;

public final class StockEntryDetEntityFactory {

    private StockEntryDetEntityFactory() {
    }

    public static List<StockEntryDetEntity> fromBulkCreate(
            StockEntryBulkCreateDto request
    ) {
        List<StockEntryDetEntity> stockEntryDetails = new ArrayList<>();
        for (StockEntryBulkLineDto line : request.DetailList) {
            stockEntryDetails.add(fromBulkLine(line, request.BulkLoadCod));
        }
        return stockEntryDetails;
    }

    public static StockEntryDetEntity fromBulkLine(
            StockEntryBulkLineDto line,
            String bulkLoadCod
    ) {
        StockEntryDetEntity stockEntryDetail = new StockEntryDetEntity();
        stockEntryDetail.ProductCod = line.ProductCod;
        stockEntryDetail.Variant = line.Variant;
        stockEntryDetail.WarehouseCod = line.WarehouseCod;
        stockEntryDetail.LotNumber = line.LotNumber;
        stockEntryDetail.ExpirationDate = line.ExpirationDate;
        stockEntryDetail.ProductUnitName = line.ProductUnitName;
        stockEntryDetail.ProductUnitFactor = line.ProductUnitFactor;
        stockEntryDetail.NumUnit = line.NumUnit;
        stockEntryDetail.NumUnitPrice = line.NumUnitPrice;
        stockEntryDetail.Observation = "Carga masiva " + bulkLoadCod
                + ", fila Excel " + line.SourceRowNumber;
        return stockEntryDetail;
    }
}
