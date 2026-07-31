package com.ccadmin.app.sale.model.dto;

import java.util.ArrayList;
import java.util.List;

public class SalePickingConfirmDto {

    public String SaleCod;
    public List<SalePickingLineDto> DetailList = new ArrayList<>();

    public SalePickingConfirmDto() {
    }
}
