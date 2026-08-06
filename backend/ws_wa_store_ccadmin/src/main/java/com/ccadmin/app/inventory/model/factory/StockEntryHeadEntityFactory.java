package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;

public final class StockEntryHeadEntityFactory {

    private StockEntryHeadEntityFactory() {
    }

    public static StockEntryHeadEntity fromBulkCreate(
            StockEntryBulkCreateDto request
    ) {
        StockEntryHeadEntity stockEntryHead = new StockEntryHeadEntity();
        stockEntryHead.StockEntryCod = request.StockEntryCod;
        stockEntryHead.StoreCod = request.StoreCod;
        stockEntryHead.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        stockEntryHead.MovementMode = StockMovementConstants.MODE_DIRECT;
        stockEntryHead.ReasonCode = BulkLoadConstants.STOCK_REASON;
        stockEntryHead.OriginStockEntryCod = null;
        stockEntryHead.ProcessStatus = StatusConst.PENDING;
        stockEntryHead.Observation =
                "Generado por carga masiva " + request.BulkLoadCod;
        return stockEntryHead;
    }
}
