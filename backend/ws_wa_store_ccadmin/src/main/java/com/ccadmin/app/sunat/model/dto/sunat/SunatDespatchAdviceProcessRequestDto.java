package com.ccadmin.app.sunat.model.dto.sunat;

import java.math.BigDecimal;
import java.util.List;

public class SunatDespatchAdviceProcessRequestDto extends SunatProcessRequestDto {
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public List<SunatDocumentLineDto> Lines;
    public String ReasonTransferCode;
    public String ReasonTransferDescription;
    public String TransportModeCode;
    public String DepartureUbigeo;
    public String DepartureAddress;
    public String ArrivalUbigeo;
    public String ArrivalAddress;
    public BigDecimal TotalWeightKg;
    public Integer NumPackages;
    public String CarrierRuc;
    public String CarrierName;
    public String VehiclePlate;
    public String DriverDocType;
    public String DriverDocNumber;
    public String DriverLicenseNumber;
}
