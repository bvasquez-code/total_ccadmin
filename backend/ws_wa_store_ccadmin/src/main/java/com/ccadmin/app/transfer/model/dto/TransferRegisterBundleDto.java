package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;

import java.util.List;

public class TransferRegisterBundleDto {

    public TransferHeadEntity transferHead;
    public List<TransferDetEntity> transferDetList;
    public TransferDocumentEntity transferDocument;
    public Boolean allowPartial;
}
