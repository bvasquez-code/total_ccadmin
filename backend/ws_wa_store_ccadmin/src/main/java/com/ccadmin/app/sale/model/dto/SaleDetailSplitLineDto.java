package com.ccadmin.app.sale.model.dto;

import java.util.Date;

public class SaleDetailSplitLineDto {

    public int ItemNumber;
    public int NumUnit;
    public String LotNumber;
    public Date ExpirationDate;

    public SaleDetailSplitLineDto(
            int itemNumber,
            int numUnit,
            String lotNumber,
            Date expirationDate
    ) {
        this.ItemNumber = itemNumber;
        this.NumUnit = numUnit;
        this.LotNumber = lotNumber;
        this.ExpirationDate = expirationDate;
    }
}
