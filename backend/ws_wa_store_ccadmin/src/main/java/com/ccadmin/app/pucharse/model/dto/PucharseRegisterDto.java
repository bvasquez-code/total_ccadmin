package com.ccadmin.app.pucharse.model.dto;

import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;

import java.util.List;

public class PucharseRegisterDto {

    public String PucharseReqCod;
    public String PucharseCod;
    public PucharseHeadEntity Headboard;
    public List<PucharseDetEntity> DetailList;
    public List<PucharseDetDeliveryEntity> DeliveryList;
}
