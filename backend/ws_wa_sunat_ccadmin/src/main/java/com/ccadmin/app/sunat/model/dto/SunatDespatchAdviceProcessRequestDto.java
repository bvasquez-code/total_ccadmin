package com.ccadmin.app.sunat.model.dto;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class SunatDespatchAdviceProcessRequestDto {
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String Series;
    public int Correlative;
    public Date IssueDate;
    public String IssueTime;
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

    public SunatElectronicDocumentDto toElectronicDocumentDto() {
        SunatElectronicDocumentDto dto = new SunatElectronicDocumentDto();
        dto.SourceModule = SourceModule;
        dto.SourceDocumentCod = SourceDocumentCod;
        dto.SourceDocumentType = SourceDocumentType;
        dto.SunatDocumentType = SunatDocumentTypeConst.GUIA_REMISION_REMITENTE;
        dto.Series = Series;
        dto.Correlative = Correlative;
        dto.IssueDate = IssueDate;
        dto.IssueTime = IssueTime;
        dto.CurrencyCod = "PEN";
        dto.Note = Note;
        dto.Supplier = Supplier;
        dto.Customer = Customer;
        dto.Totals = new SunatDocumentTotalsDto();
        dto.Lines = Lines;
        dto.ReasonTransferCode = ReasonTransferCode;
        dto.ReasonTransferDescription = ReasonTransferDescription;
        dto.TransportModeCode = TransportModeCode;
        dto.DepartureUbigeo = DepartureUbigeo;
        dto.DepartureAddress = DepartureAddress;
        dto.ArrivalUbigeo = ArrivalUbigeo;
        dto.ArrivalAddress = ArrivalAddress;
        dto.TotalWeightKg = TotalWeightKg;
        dto.NumPackages = NumPackages;
        dto.CarrierRuc = CarrierRuc;
        dto.CarrierName = CarrierName;
        dto.VehiclePlate = VehiclePlate;
        dto.DriverDocType = DriverDocType;
        dto.DriverDocNumber = DriverDocNumber;
        dto.DriverLicenseNumber = DriverLicenseNumber;
        return dto;
    }
}
