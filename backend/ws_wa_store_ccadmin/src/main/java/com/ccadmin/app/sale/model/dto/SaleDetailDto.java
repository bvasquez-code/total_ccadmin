package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;

import java.util.List;

public class SaleDetailDto {

    public SaleHeadEntity Headboard;
    public SaleDocumentEntity SaleDocument;

    public List<SaleDetEntity> DetailList;
    public List<SalePaymentEntity> DetailPayment;
    public CreditNoteDetailDto CreditNoteDetail;

}
