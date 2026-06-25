package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;

import java.util.List;

public class TransferDetailDto {

    public TransferHeadEntity transferHeadTe;
    public TransferHeadEntity transferHeadTs;
    public List<TransferDetEntity> transferDetTeList;
    public List<TransferDetEntity> transferDetTsList;
    public List<TransferDocumentEntity> transferDocumentList;
}
