package com.ccadmin.app.pucharse.model.dto;

import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;

import java.util.List;

public class PucharseDetLotConfirmDto {

    public PucharseDetEntity pucharseDet;
    public List<PucharseDetEntity> lotDetailList;
    public String WarehouseCod;
}
