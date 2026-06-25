package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;

import java.util.List;

public class TransferReceiveDto {

    public String transferCod;
    public String user;
    public String observation;
    public String typeOperation;
    public List<TransferDetEntity> detailListReceive;
}
