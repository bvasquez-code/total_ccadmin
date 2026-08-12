package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.*;

import java.util.List;

public class SaleDetailDto {

    public SaleHeadEntity Headboard;
    public SaleDocumentEntity SaleDocument;
    public List<SaleDocumentEntity> SaleDocumentList;
    public SaleChannelEntity SaleChannel;
    public SaleDeliveryEntity SaleDelivery;

    public List<SaleDetEntity> DetailList;
    public List<SalePaymentEntity> DetailPayment;
    public CreditNoteDetailDto CreditNoteDetail;

}
