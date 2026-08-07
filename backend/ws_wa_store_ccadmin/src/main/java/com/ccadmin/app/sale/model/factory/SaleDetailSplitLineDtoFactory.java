package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.SaleDetailSplitLineDto;

import java.util.Date;

public final class SaleDetailSplitLineDtoFactory {

    private SaleDetailSplitLineDtoFactory() {
    }

    public static SaleDetailSplitLineDto fromPicking(
            int itemNumber,
            int numUnit,
            String lotNumber,
            Date expirationDate
    ) {
        SaleDetailSplitLineDto result = new SaleDetailSplitLineDto();
        result.ItemNumber = itemNumber;
        result.NumUnit = numUnit;
        result.LotNumber = lotNumber;
        result.ExpirationDate = expirationDate;
        return result;
    }
}
