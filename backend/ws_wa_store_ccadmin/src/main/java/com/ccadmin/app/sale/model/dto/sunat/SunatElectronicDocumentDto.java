package com.ccadmin.app.sale.model.dto.sunat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class SunatElectronicDocumentDto {
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public int Correlative;
    public Date IssueDate;
    public String IssueTime;
    public String CurrencyCod;
    public String OperationTypeCode = "0101";
    public String PaymentCondition = "Contado";
    public List<SunatPaymentTermDto> PaymentTerms;
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public SunatDiscrepancyResponseDto DiscrepancyResponse;
    public List<SunatRelatedDocumentDto> RelatedDocuments;
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
