package com.ccadmin.app.inventory.model.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class StockEntryBulkResultDto {
    public String StockEntryCod;
    public Map<Integer, Integer> ItemNumberByReference = new LinkedHashMap<>();
}
