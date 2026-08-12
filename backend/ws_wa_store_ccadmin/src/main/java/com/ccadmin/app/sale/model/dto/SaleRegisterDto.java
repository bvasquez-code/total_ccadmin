package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;

import java.util.List;

public class SaleRegisterDto {

    public SaleHeadEntity Headboard;

    public SaleBillingEntity SaleBilling;

    public List<SaleDetEntity> DetailList;
}
