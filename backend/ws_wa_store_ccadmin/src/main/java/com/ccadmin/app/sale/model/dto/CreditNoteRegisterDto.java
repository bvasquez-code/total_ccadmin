package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;


import java.util.List;

public class CreditNoteRegisterDto {

    public CreditNoteHeadEntity Headboard;

    public List<CreditNoteDetEntity> DetailList;
    public CreditNoteDocumentEntity Document;

}
