package com.ccadmin.app.inventory.model.dto;

import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;

import java.util.ArrayList;
import java.util.List;

public class StockEntryRegisterDto {
    public StockEntryHeadEntity Head = new StockEntryHeadEntity();
    public List<StockEntryDetEntity> DetailList = new ArrayList<>();
}
