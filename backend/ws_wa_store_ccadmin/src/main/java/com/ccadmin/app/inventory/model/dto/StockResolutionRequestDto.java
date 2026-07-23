package com.ccadmin.app.inventory.model.dto;

import java.util.ArrayList;
import java.util.List;

public class StockResolutionRequestDto {
    public String Code;
    public List<StockResolutionLineDto> DetailList = new ArrayList<>();
}
