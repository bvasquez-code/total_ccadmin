package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentLineDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentTotalsDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatPartyDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.shared.StoreShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Service
public class SaleSunatPayloadBuildService {

    @Autowired
    private StoreShared storeShared;

    public SunatElectronicDocumentDto build(SaleDetailDto saleDetail) {
        if (saleDetail == null || saleDetail.Headboard == null || saleDetail.SaleDocument == null) {
            throw new IllegalArgumentException("Venta confirmada requerida para SUNAT");
        }
        SaleHeadEntity head = saleDetail.Headboard;
        SaleDocumentEntity document = saleDetail.SaleDocument;
        DocumentNumber documentNumber = parseDocumentNumber(document.DocumentCod);

        SunatElectronicDocumentDto dto = new SunatElectronicDocumentDto();
        dto.SourceModule = "SALE";
        dto.SourceDocumentCod = head.SaleCod;
        dto.SourceDocumentType = "SALE";
        dto.SunatDocumentType = resolveSunatDocumentType(documentNumber.series);
        dto.Series = documentNumber.series;
        dto.Correlative = documentNumber.correlative;
        dto.IssueDate = head.ModifyDate == null ? new Date() : head.ModifyDate;
        dto.CurrencyCod = head.CurrencyCod;
        dto.Supplier = buildSupplier(head.StoreCod);
        dto.Customer = buildCustomer(head.Client);
        dto.Totals = buildTotals(head);
        dto.Lines = saleDetail.DetailList.stream()
                .map(line -> buildLine(line, head))
                .toList();
        return dto;
    }

    public boolean isInvoiceOrReceipt(SaleDetailDto saleDetail) {
        if (saleDetail == null || saleDetail.SaleDocument == null || saleDetail.SaleDocument.DocumentCod == null) {
            return false;
        }
        String series = saleDetail.SaleDocument.DocumentCod.split("-")[0];
        return series.startsWith("F") || series.startsWith("B");
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

    private SunatPartyDto buildCustomer(ClientEntity client) {
        if (client == null || client.Person == null) {
            throw new IllegalArgumentException("Cliente requerido para enviar venta a SUNAT");
        }
        PersonEntity person = client.Person;
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

    private SunatDocumentTotalsDto buildTotals(SaleHeadEntity head) {
        SunatDocumentTotalsDto totals = new SunatDocumentTotalsDto();
        totals.TaxableAmount = amount(head.NumTotalPriceNoTax);
        totals.TaxAmount = amount(head.NumTotalTax);
        totals.DiscountTotal = amount(head.NumDiscount);
        totals.LineExtensionAmount = amount(head.NumTotalPriceNoTax);
        totals.TaxInclusiveAmount = amount(head.NumTotalPrice);
        totals.PayableAmount = amount(head.NumTotalPrice);
        return totals;
    }

    private SunatDocumentLineDto buildLine(SaleDetEntity line, SaleHeadEntity head) {
        SunatDocumentLineDto dto = new SunatDocumentLineDto();
        dto.ItemNumber = line.ItemNumber;
        dto.ProductCode = line.ProductCod;
        dto.Description = line.Product == null ? line.ProductCod : line.Product.ProductName;
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

    private String resolveSunatDocumentType(String series) {
        if (series.startsWith("F")) return "01";
        if (series.startsWith("B")) return "03";
        throw new IllegalArgumentException("Serie no corresponde a factura o boleta: " + series);
    }

    private DocumentNumber parseDocumentNumber(String documentCod) {
        if (documentCod == null || !documentCod.contains("-")) {
            throw new IllegalArgumentException("Documento de venta invalido para SUNAT");
        }
        String[] parts = documentCod.split("-");
        return new DocumentNumber(parts[0], Integer.parseInt(parts[1]));
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null) return null;
        return switch (documentType.trim().toUpperCase()) {
            case "DNI" -> "1";
            case "RUC" -> "6";
            case "CE" -> "4";
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
