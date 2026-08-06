package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.inventory.model.dto.StockEntryRegisterDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;

import java.util.List;

public final class StockEntryRegisterDtoFactory {

    private StockEntryRegisterDtoFactory() {
    }

    public static StockEntryRegisterDto fromEntities(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails
    ) {
        StockEntryRegisterDto result = new StockEntryRegisterDto();
        result.Head = stockEntryHead;
        result.DetailList = stockEntryDetails;
        return result;
    }
}
