package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.inventory.model.dto.StockExitRegisterDto;
import com.ccadmin.app.inventory.model.entity.StockExitDetEntity;
import com.ccadmin.app.inventory.model.entity.StockExitHeadEntity;

import java.util.List;

public final class StockExitRegisterDtoFactory {

    private StockExitRegisterDtoFactory() {
    }

    public static StockExitRegisterDto fromEntities(
            StockExitHeadEntity stockExitHead,
            List<StockExitDetEntity> stockExitDetails
    ) {
        StockExitRegisterDto result = new StockExitRegisterDto();
        result.Head = stockExitHead;
        result.DetailList = stockExitDetails;
        return result;
    }
}
