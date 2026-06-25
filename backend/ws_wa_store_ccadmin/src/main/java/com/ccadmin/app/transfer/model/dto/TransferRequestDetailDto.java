package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.*;

import java.util.List;

public class TransferRequestDetailDto {
    public TransferRequestHeadEntity transferHeadRequest;
    public TransferHeadEntity transferHead;
    public List<TransferRequestDetEntity> transferDetRequestList;
    public List<TransferDetEntity> transferDetList;
    public List<TransferDocumentEntity> transferDocumentList;
}
