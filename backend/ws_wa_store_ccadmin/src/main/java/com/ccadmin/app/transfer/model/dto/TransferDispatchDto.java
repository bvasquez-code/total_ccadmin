package com.ccadmin.app.transfer.model.dto;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;

import java.util.List;

public class TransferDispatchDto {

    public String transferCod;
    public String user;
    public String transportModeCod;
    public String reasonTransferCod;
    public String vehiclePlate;
    public String driverDocType;
    public String driverDocNumber;
    public String driverLicenseNumber;
    public String carrierRuc;
    public String carrierName;
    public String observation;
    public List<TransferDetEntity> detailListRequest;
}
