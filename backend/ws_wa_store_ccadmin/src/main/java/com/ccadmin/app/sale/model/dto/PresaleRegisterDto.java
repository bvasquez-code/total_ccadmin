package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;

import java.util.List;

public class PresaleRegisterDto {

    public PresaleHeadEntity Headboard;

    public List<PresaleDetEntity> DetailList;

    public PresaleChannelEntity PresaleChannel;

    public SaleBillingEntity SaleBilling;

    public String CreditNoteCod;

}
