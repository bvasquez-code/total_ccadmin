package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;

import java.util.List;

public final class StockEntryBulkResultDtoFactory {

    private StockEntryBulkResultDtoFactory() {
    }

    public static StockEntryBulkResultDto fromConfirmedBulk(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryBulkLineDto> sourceLines,
            List<StockEntryDetEntity> stockEntryDetails
    ) {
        StockEntryBulkResultDto result = new StockEntryBulkResultDto();
        result.StockEntryCod = stockEntryHead.StockEntryCod;
        for (int index = 0; index < stockEntryDetails.size(); index++) {
            result.ItemNumberByReference.put(
                    sourceLines.get(index).ReferenceItemNumber,
                    stockEntryDetails.get(index).ItemNumber
            );
        }
        return result;
    }
}
