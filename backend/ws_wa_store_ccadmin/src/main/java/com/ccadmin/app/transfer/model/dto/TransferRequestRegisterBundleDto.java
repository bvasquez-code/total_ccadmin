package com.ccadmin.app.transfer.model.dto;



import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestHeadEntity;

import java.util.List;

public class TransferRequestRegisterBundleDto {

    public TransferRequestHeadEntity transferHead;
    public List<TransferRequestDetEntity> transferDetList;
    public TransferDocumentEntity transferDocument;
    public Boolean allowPartial;
}
