package com.ccadmin.app.inventory.model.dto;

import java.util.ArrayList;
import java.util.List;

public class StockEntryBulkCreateDto {
    public String StockEntryCod;
    public String StoreCod;
    public String BulkLoadCod;
    public List<StockEntryBulkLineDto> DetailList = new ArrayList<>();
}
