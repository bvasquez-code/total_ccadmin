package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;

import java.util.List;

public class TransferDetRegisterMassiveDto {

    public List<TransferDetEntity> transferDetList;

    public TransferDetRegisterMassiveDto(){

    }

    public TransferDetRegisterMassiveDto(List<TransferDetEntity> transferDetList) {
        this.transferDetList = transferDetList;
    }
}
