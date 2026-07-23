package com.ccadmin.app.inventory.model.dto;

import com.ccadmin.app.inventory.model.entity.StockExitDetEntity;
import com.ccadmin.app.inventory.model.entity.StockExitHeadEntity;

import java.util.ArrayList;
import java.util.List;

public class StockExitRegisterDto {
    public StockExitHeadEntity Head = new StockExitHeadEntity();
    public List<StockExitDetEntity> DetailList = new ArrayList<>();
}
