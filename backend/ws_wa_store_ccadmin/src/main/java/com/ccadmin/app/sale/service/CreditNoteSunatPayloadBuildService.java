package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.CreditNoteDetDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDiscrepancyResponseDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentLineDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentTotalsDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatPartyDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatRelatedDocumentDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.shared.StoreShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CreditNoteSunatPayloadBuildService {

    private static final String SUNAT_FACTURA = "01";
    private static final String SUNAT_BOLETA = "03";
    private static final String SUNAT_NOTA_CREDITO = "07";
    private static final BigDecimal ANONYMOUS_BOLETA_LIMIT = new BigDecimal("700.00");

    @Autowired
    private StoreShared storeShared;

    public SunatElectronicDocumentDto build(CreditNoteDetailDto creditNoteDetail) {
        if (creditNoteDetail == null || creditNoteDetail.Headboard == null || creditNoteDetail.Document == null) {
            throw new IllegalArgumentException("Nota de credito confirmada requerida para SUNAT");
        }
        if (creditNoteDetail.DocumentReference == null || creditNoteDetail.DocumentReference.DocumentCod == null) {
            throw new IllegalArgumentException("Documento de venta afectado requerido para SUNAT");
        }

        CreditNoteHeadEntity head = creditNoteDetail.Headboard;
        CreditNoteDocumentEntity document = creditNoteDetail.Document;
        SaleDocumentEntity referenceDocument = creditNoteDetail.DocumentReference;
        DocumentNumber documentNumber = parseDocumentNumber(document.DocumentCod);
        String relatedDocumentType = resolveRelatedDocumentType(referenceDocument.DocumentCod);

        SunatElectronicDocumentDto dto = new SunatElectronicDocumentDto();
        dto.SourceModule = "CREDIT_NOTE";
        dto.SourceDocumentCod = head.CreditNoteCod;
        dto.SourceDocumentType = "CREDIT_NOTE";
        dto.SunatDocumentType = SUNAT_NOTA_CREDITO;
        dto.Series = documentNumber.series;
        dto.Correlative = documentNumber.correlative;
        dto.IssueDate = head.ModifyDate == null ? new Date() : head.ModifyDate;
        dto.CurrencyCod = head.CurrencyCod;
        dto.Supplier = buildSupplier(head.StoreCod);
        dto.Totals = buildTotals(head);
        dto.Customer = buildCustomer(creditNoteDetail.Client, relatedDocumentType, dto.Totals.PayableAmount);
        dto.DiscrepancyResponse = buildDiscrepancyResponse(head, referenceDocument.DocumentCod);
        dto.RelatedDocuments = buildRelatedDocuments(referenceDocument.DocumentCod, relatedDocumentType);
        dto.Lines = new ArrayList<>(creditNoteDetail.DetailList.stream()
                .map(this::buildLine)
                .toList());
        reconcileLineTotals(dto);
        return dto;
    }

    private SunatPartyDto buildSupplier(String storeCod) {
        StoreInfoDto storeInfo = this.storeShared.findStoreInfo(storeCod);
        CompanyEntity company = storeInfo.Company;
        SunatPartyDto supplier = new SunatPartyDto();
        supplier.DocumentType = "6";
        supplier.DocumentNumber = company.TaxId;
        supplier.LegalName = company.LegalName;
        supplier.TradeName = company.TradeName;
        supplier.Address = company.FiscalAddress == null || company.FiscalAddress.isBlank() ? company.Address : company.FiscalAddress;
        supplier.UbigeoCod = company.UbigeoCod;
        supplier.AddressTypeCode = normalizeAddressTypeCode(storeInfo.Store == null ? null : storeInfo.Store.SunatAddressTypeCode);
        supplier.Department = company.Department;
        supplier.Province = company.Province;
        supplier.District = company.District;
        supplier.CountryCode = company.CountryCode == null ? "PE" : company.CountryCode;
        return supplier;
    }

    private SunatPartyDto buildCustomer(ClientEntity client, String relatedDocumentType, BigDecimal payableAmount) {
        if (client == null || client.Person == null) {
            return buildAnonymousCustomerOrThrow(relatedDocumentType, payableAmount);
        }
        PersonEntity person = client.Person;
        if (!hasCustomerIdentity(person)) {
            return buildAnonymousCustomerOrThrow(relatedDocumentType, payableAmount);
        }
        SunatPartyDto customer = new SunatPartyDto();
        customer.DocumentType = normalizeDocumentType(person.DocumentType);
        customer.DocumentNumber = person.DocumentNum;
        customer.LegalName = firstNotBlank(person.BusinessName, fullName(person), person.CommercialName);
        customer.TradeName = person.CommercialName;
        customer.Address = person.Address;
        customer.UbigeoCod = person.UbigeoCod;
        customer.CountryCode = "PE";
        return customer;
    }

    private SunatPartyDto buildAnonymousCustomerOrThrow(String relatedDocumentType, BigDecimal payableAmount) {
        if (SUNAT_FACTURA.equals(relatedDocumentType)) {
            throw new IllegalArgumentException("Nota de credito de factura requiere cliente con RUC para SUNAT");
        }
        if (!SUNAT_BOLETA.equals(relatedDocumentType)) {
            throw new IllegalArgumentException("Cliente requerido para enviar nota de credito a SUNAT");
        }
        if (amount(payableAmount).compareTo(ANONYMOUS_BOLETA_LIMIT) > 0) {
            throw new IllegalArgumentException("Nota de credito de boleta mayor a S/ 700 requiere datos del cliente para SUNAT");
        }
        SunatPartyDto customer = new SunatPartyDto();
        customer.DocumentType = "1";
        customer.DocumentNumber = "00000000";
        customer.LegalName = "CLIENTES VARIOS";
        customer.TradeName = "CLIENTES VARIOS";
        customer.CountryCode = "PE";
        return customer;
    }

    private SunatDiscrepancyResponseDto buildDiscrepancyResponse(CreditNoteHeadEntity head, String referenceDocumentNumber) {
        SunatDiscrepancyResponseDto dto = new SunatDiscrepancyResponseDto();
        dto.ReferenceDocumentNumber = referenceDocumentNumber;
        dto.ResponseCode = "T".equals(head.TypeCreditNote) ? "06" : "07";
        dto.Description = firstNotBlank(head.Commenter, "DEVOLUCION DE PRODUCTOS");
        return dto;
    }

    private List<SunatRelatedDocumentDto> buildRelatedDocuments(String documentNumber, String documentType) {
        SunatRelatedDocumentDto dto = new SunatRelatedDocumentDto();
        dto.DocumentNumber = documentNumber;
        dto.DocumentType = documentType;
        return List.of(dto);
    }

    private SunatDocumentTotalsDto buildTotals(CreditNoteHeadEntity head) {
        SunatDocumentTotalsDto totals = new SunatDocumentTotalsDto();
        BigDecimal total = amount(head.NumTotalPrice);
        BigDecimal taxable = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        totals.TaxableAmount = taxable;
        totals.TaxAmount = total.subtract(taxable).setScale(2, RoundingMode.HALF_UP);
        totals.LineExtensionAmount = taxable;
        totals.TaxInclusiveAmount = total;
        totals.PayableAmount = total;
        return totals;
    }

    private SunatDocumentLineDto buildLine(CreditNoteDetDto detailDto) {
        CreditNoteDetEntity line = detailDto.CreditNoteDet;
        SunatDocumentLineDto dto = new SunatDocumentLineDto();
        dto.ItemNumber = line.ItemNumber;
        dto.ProductCode = line.ProductCod;
        dto.Description = detailDto.Product == null ? line.ProductCod : detailDto.Product.ProductName;
        dto.UnitCode = line.ProductUnitName == null || line.ProductUnitName.isBlank() ? "NIU" : line.ProductUnitName;
        dto.Quantity = BigDecimal.valueOf(line.NumUnit);
        dto.LineExtensionAmount = amount(line.NumTotalPrice).divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        dto.TaxableAmount = dto.LineExtensionAmount;
        dto.TaxAmount = amount(line.NumTotalPrice).subtract(dto.LineExtensionAmount).setScale(2, RoundingMode.HALF_UP);
        dto.UnitPrice = dto.Quantity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : dto.LineExtensionAmount.divide(dto.Quantity, 2, RoundingMode.HALF_UP);
        dto.PriceAmount = amount(line.NumUnitPriceSale);
        dto.TaxPercent = BigDecimal.valueOf(18);
        return dto;
    }

    private void reconcileLineTotals(SunatElectronicDocumentDto dto) {
        if (dto.Lines == null || dto.Lines.isEmpty() || dto.Totals == null) {
            return;
        }
        BigDecimal lineTotal = dto.Lines.stream()
                .map(line -> amount(line.LineExtensionAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxTotal = dto.Lines.stream()
                .map(line -> amount(line.TaxAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal lineDifference = amount(dto.Totals.LineExtensionAmount).subtract(lineTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxDifference = amount(dto.Totals.TaxAmount).subtract(taxTotal).setScale(2, RoundingMode.HALF_UP);
        if (lineDifference.compareTo(BigDecimal.ZERO) == 0 && taxDifference.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal tolerance = BigDecimal.valueOf(dto.Lines.size()).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        if (lineDifference.abs().compareTo(tolerance) > 0 || taxDifference.abs().compareTo(tolerance) > 0) {
            throw new IllegalArgumentException("Diferencia de totales SUNAT supera tolerancia de redondeo");
        }

        SunatDocumentLineDto lastLine = dto.Lines.get(dto.Lines.size() - 1);
        lastLine.LineExtensionAmount = amount(lastLine.LineExtensionAmount).add(lineDifference).setScale(2, RoundingMode.HALF_UP);
        lastLine.TaxableAmount = lastLine.LineExtensionAmount;
        lastLine.TaxAmount = amount(lastLine.TaxAmount).add(taxDifference).setScale(2, RoundingMode.HALF_UP);
        if (lastLine.Quantity != null && lastLine.Quantity.compareTo(BigDecimal.ZERO) > 0) {
            lastLine.UnitPrice = lastLine.LineExtensionAmount.divide(lastLine.Quantity, 2, RoundingMode.HALF_UP);
        }
    }

    private String resolveRelatedDocumentType(String series) {
        if (series.startsWith("F")) return SUNAT_FACTURA;
        if (series.startsWith("B")) return SUNAT_BOLETA;
        throw new IllegalArgumentException("Documento afectado no corresponde a factura o boleta: " + series);
    }

    private DocumentNumber parseDocumentNumber(String documentCod) {
        if (documentCod == null || !documentCod.contains("-")) {
            throw new IllegalArgumentException("Documento de nota de credito invalido para SUNAT");
        }
        String[] parts = documentCod.split("-");
        return new DocumentNumber(parts[0], Integer.parseInt(parts[1]));
    }

    private boolean hasCustomerIdentity(PersonEntity person) {
        return person.DocumentType != null && !person.DocumentType.isBlank()
                && person.DocumentNum != null && !person.DocumentNum.isBlank()
                && firstNotBlank(person.BusinessName, fullName(person), person.CommercialName) != null;
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null) return null;
        return switch (documentType.trim().toUpperCase()) {
            case "DNI", "01", "1" -> "1";
            case "RUC", "06", "6" -> "6";
            case "CE", "04", "4" -> "4";
            case "PAS", "PASAPORTE", "07", "7" -> "7";
            default -> documentType.trim();
        };
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeAddressTypeCode(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        String code = value.trim();
        if (!code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Codigo SUNAT de local anexo invalido: " + value);
        }
        return code;
    }

    private String fullName(PersonEntity person) {
        String value = ((person.Names == null ? "" : person.Names) + " " + (person.LastNames == null ? "" : person.LastNames)).trim();
        return value.isBlank() ? null : value;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private record DocumentNumber(String series, int correlative) {
    }
}
