package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDocumentLineDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDocumentTotalsDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatPartyDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import com.ccadmin.app.sale.model.constants.SaleConstants;
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
public class SaleSunatPayloadBuildService {

    private static final String SUNAT_FACTURA = "01";
    private static final String SUNAT_BOLETA = "03";
    private static final BigDecimal ANONYMOUS_BOLETA_LIMIT = new BigDecimal("700.00");

    @Autowired
    private StoreShared storeShared;

    public SunatInvoiceProcessRequestDto buildInvoice(SaleDetailDto saleDetail) {
        SalePayload payload = buildPayload(saleDetail);
        if (!SUNAT_FACTURA.equals(payload.sunatDocumentType())) {
            throw new IllegalArgumentException("Serie no corresponde a factura: " + payload.series());
        }
        SunatInvoiceProcessRequestDto dto = new SunatInvoiceProcessRequestDto();
        dto.SourceModule = payload.sourceModule();
        dto.SourceDocumentCod = payload.sourceDocumentCod();
        dto.SourceDocumentType = payload.sourceDocumentType();
        dto.StoreCod = saleDetail.Headboard.StoreCod;
        dto.AuditUserCod = saleDetail.Headboard.CreationUser;
        dto.Series = payload.series();
        dto.Correlative = payload.correlative();
        dto.IssueDate = payload.issueDate();
        dto.CurrencyCod = payload.currencyCod();
        dto.PaymentCondition = payload.paymentCondition();
        dto.Supplier = payload.supplier();
        dto.Customer = payload.customer();
        dto.Totals = payload.totals();
        dto.Lines = payload.lines();
        return dto;
    }

    public SunatReceiptProcessRequestDto buildReceipt(SaleDetailDto saleDetail) {
        SalePayload payload = buildPayload(saleDetail);
        if (!SUNAT_BOLETA.equals(payload.sunatDocumentType())) {
            throw new IllegalArgumentException("Serie no corresponde a boleta: " + payload.series());
        }
        SunatReceiptProcessRequestDto dto = new SunatReceiptProcessRequestDto();
        dto.SourceModule = payload.sourceModule();
        dto.SourceDocumentCod = payload.sourceDocumentCod();
        dto.SourceDocumentType = payload.sourceDocumentType();
        dto.StoreCod = saleDetail.Headboard.StoreCod;
        dto.AuditUserCod = saleDetail.Headboard.CreationUser;
        dto.Series = payload.series();
        dto.Correlative = payload.correlative();
        dto.IssueDate = payload.issueDate();
        dto.CurrencyCod = payload.currencyCod();
        dto.PaymentCondition = payload.paymentCondition();
        dto.Supplier = payload.supplier();
        dto.Customer = payload.customer();
        dto.Totals = payload.totals();
        dto.Lines = payload.lines();
        return dto;
    }

    private SalePayload buildPayload(SaleDetailDto saleDetail) {
        if (saleDetail == null || saleDetail.Headboard == null || saleDetail.SaleDocument == null) {
            throw new IllegalArgumentException("Venta confirmada requerida para SUNAT");
        }
        SaleHeadEntity head = saleDetail.Headboard;
        SaleDocumentEntity document = saleDetail.SaleDocument;
        DocumentNumber documentNumber = parseDocumentNumber(document.DocumentCod);
        String sunatDocumentType = resolveSunatDocumentType(documentNumber.series);
        SunatDocumentTotalsDto totals = buildTotals(head);
        List<SunatDocumentLineDto> lines = new ArrayList<>(saleDetail.DetailList.stream()
                .map(line -> buildLine(line, head))
                .toList());
        reconcileLineTotals(lines, totals);
        return new SalePayload(
                "SALE",
                head.SaleCod,
                "SALE",
                sunatDocumentType,
                documentNumber.series,
                documentNumber.correlative,
                document.IssueDate == null
                        ? (document.CreationDate == null ? new Date() : document.CreationDate)
                        : document.IssueDate,
                head.CurrencyCod,
                "Contado",
                buildSupplier(head.StoreCod),
                buildCustomer(saleDetail.SaleBilling, sunatDocumentType, totals.PayableAmount),
                totals,
                lines
        );
    }

    public boolean isInvoiceOrReceipt(SaleDetailDto saleDetail) {
        if (saleDetail == null || saleDetail.SaleDocument == null || saleDetail.SaleDocument.DocumentCod == null) {
            return false;
        }
        String documentType = saleDetail.SaleDocument.DocumentType;
        return SaleConstants.DOCUMENT_TYPE_INVOICE.equals(documentType)
                || SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(documentType);
    }

    public boolean isInvoice(SaleDetailDto saleDetail) {
        return saleDetail != null
                && saleDetail.SaleDocument != null
                && SaleConstants.DOCUMENT_TYPE_INVOICE.equals(saleDetail.SaleDocument.DocumentType);
    }

    public boolean isReceipt(SaleDetailDto saleDetail) {
        return saleDetail != null
                && saleDetail.SaleDocument != null
                && SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(saleDetail.SaleDocument.DocumentType);
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

    private SunatPartyDto buildCustomer(
            SaleBillingEntity saleBilling,
            String sunatDocumentType,
            BigDecimal payableAmount
    ) {
        this.validateCustomerForDocument(saleBilling, sunatDocumentType, payableAmount);
        if (!hasCustomerIdentity(saleBilling)) {
            return buildAnonymousCustomerOrThrow(sunatDocumentType, payableAmount);
        }
        SunatPartyDto customer = new SunatPartyDto();
        customer.DocumentType = normalizeDocumentType(saleBilling.DocumentType);
        customer.DocumentNumber = saleBilling.DocumentNum;
        customer.LegalName = saleBilling.LegalName;
        customer.TradeName = saleBilling.CommercialName;
        customer.Address = saleBilling.Address;
        customer.UbigeoCod = saleBilling.UbigeoCod;
        customer.CountryCode = "PE";
        return customer;
    }

    public void validateCustomerForDocument(
            SaleBillingEntity saleBilling,
            String documentType,
            BigDecimal payableAmount
    ) {
        if (SUNAT_FACTURA.equals(documentType)) {
            if (saleBilling == null
                    || !"6".equals(normalizeDocumentType(saleBilling.DocumentType))
                    || saleBilling.DocumentNum == null
                    || !saleBilling.DocumentNum.trim().matches("\\d{11}")
                    || !hasCustomerIdentity(saleBilling)) {
                throw new IllegalArgumentException(
                        "La factura requiere una persona con RUC valido y razon social"
                );
            }
            return;
        }

        if (!SUNAT_BOLETA.equals(documentType)) {
            throw new IllegalArgumentException("Tipo de documento fiscal no permitido: " + documentType);
        }
        if (!hasCustomerIdentity(saleBilling)
                && amount(payableAmount).compareTo(ANONYMOUS_BOLETA_LIMIT) > 0) {
            throw new IllegalArgumentException(
                    "La boleta mayor a S/ 700 requiere una persona identificada"
            );
        }
    }

    private SunatPartyDto buildAnonymousCustomerOrThrow(String sunatDocumentType, BigDecimal payableAmount) {
        if (SUNAT_FACTURA.equals(sunatDocumentType)) {
            throw new IllegalArgumentException("Factura requiere cliente con RUC para enviar a SUNAT");
        }
        if (!SUNAT_BOLETA.equals(sunatDocumentType)) {
            throw new IllegalArgumentException("Cliente requerido para enviar documento a SUNAT");
        }
        if (amount(payableAmount).compareTo(ANONYMOUS_BOLETA_LIMIT) > 0) {
            throw new IllegalArgumentException("Boleta mayor a S/ 700 requiere datos del cliente para SUNAT");
        }
        SunatPartyDto customer = new SunatPartyDto();
        customer.DocumentType = "1";
        customer.DocumentNumber = "00000000";
        customer.LegalName = "CLIENTES VARIOS";
        customer.TradeName = "CLIENTES VARIOS";
        customer.CountryCode = "PE";
        return customer;
    }

    private boolean hasCustomerIdentity(SaleBillingEntity saleBilling) {
        return saleBilling != null
                && saleBilling.DocumentType != null && !saleBilling.DocumentType.isBlank()
                && saleBilling.DocumentNum != null && !saleBilling.DocumentNum.isBlank()
                && saleBilling.LegalName != null && !saleBilling.LegalName.isBlank();
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
        dto.UnitCode = normalizeSunatUnitCode(line.ProductUnitName);
        dto.Quantity = BigDecimal.valueOf(line.NumUnit);
        dto.LineExtensionAmount = detailSubTotal(line);
        dto.TaxableAmount = dto.LineExtensionAmount;
        dto.TaxAmount = detailTax(line, dto.LineExtensionAmount);
        dto.UnitPrice = dto.Quantity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : dto.LineExtensionAmount.divide(dto.Quantity, 2, RoundingMode.HALF_UP);
        dto.PriceAmount = amount(line.NumUnitPriceSale);
        applyMainTaxSnapshot(dto, line);
        return dto;
    }

    private BigDecimal detailSubTotal(SaleDetEntity line) {
        if (amount(line.NumPriceSubTotal).compareTo(BigDecimal.ZERO) > 0 || amount(line.NumTotalPrice).compareTo(BigDecimal.ZERO) == 0) {
            return amount(line.NumPriceSubTotal);
        }
        if (amount(line.NumTotalTax).compareTo(BigDecimal.ZERO) > 0) {
            return amount(line.NumTotalPrice).subtract(amount(line.NumTotalTax)).setScale(2, RoundingMode.HALF_UP);
        }
        return amount(line.NumTotalPrice);
    }

    private BigDecimal detailTax(SaleDetEntity line, BigDecimal lineExtensionAmount) {
        if (amount(line.NumTotalTax).compareTo(BigDecimal.ZERO) > 0 || amount(line.NumTotalPrice).compareTo(BigDecimal.ZERO) == 0) {
            return amount(line.NumTotalTax);
        }
        BigDecimal taxDetailTotal = line.TaxDetailList == null ? BigDecimal.ZERO : line.TaxDetailList.stream()
                .filter(tax -> !"S".equals(tax.IsInformative))
                .map(tax -> amount(tax.TaxAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (taxDetailTotal.compareTo(BigDecimal.ZERO) > 0) {
            return taxDetailTotal;
        }
        return amount(line.NumTotalPrice).subtract(amount(lineExtensionAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    private void applyMainTaxSnapshot(SunatDocumentLineDto dto, SaleDetEntity line) {
        SaleDetTaxEntity mainTax = line.TaxDetailList == null ? null : line.TaxDetailList.stream()
                .filter(tax -> tax.TaxAffectationCod != null && !tax.TaxAffectationCod.isBlank())
                .findFirst()
                .orElse(null);
        if (mainTax == null) {
            dto.TaxPercent = BigDecimal.ZERO;
            return;
        }
        dto.TaxPercent = amount(mainTax.TaxRateValue);
        dto.TaxExemptionReasonCode = mainTax.TaxAffectationCod;
        dto.TaxSchemeId = mainTax.SunatTaxCod == null || mainTax.SunatTaxCod.isBlank() ? mainTax.TaxCod : mainTax.SunatTaxCod;
        dto.TaxSchemeName = mainTax.TaxName;
        dto.TaxTypeCode = "VAT";
        dto.TaxCategoryCode = switch (mainTax.TaxAffectationCod) {
            case "10" -> "S";
            case "20" -> "E";
            case "30" -> "O";
            case "40" -> "G";
            default -> dto.TaxCategoryCode;
        };
    }

    private void reconcileLineTotals(List<SunatDocumentLineDto> lines, SunatDocumentTotalsDto totals) {
        if (lines == null || lines.isEmpty() || totals == null) {
            return;
        }
        BigDecimal lineTotal = lines.stream()
                .map(line -> amount(line.LineExtensionAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxTotal = lines.stream()
                .map(line -> amount(line.TaxAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal lineDifference = amount(totals.LineExtensionAmount).subtract(lineTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxDifference = amount(totals.TaxAmount).subtract(taxTotal).setScale(2, RoundingMode.HALF_UP);
        if (lineDifference.compareTo(BigDecimal.ZERO) == 0 && taxDifference.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal tolerance = BigDecimal.valueOf(lines.size()).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        if (lineDifference.abs().compareTo(tolerance) > 0 || taxDifference.abs().compareTo(tolerance) > 0) {
            throw new IllegalArgumentException("Diferencia de totales SUNAT supera tolerancia de redondeo");
        }

        SunatDocumentLineDto lastLine = lines.get(lines.size() - 1);
        lastLine.LineExtensionAmount = amount(lastLine.LineExtensionAmount).add(lineDifference).setScale(2, RoundingMode.HALF_UP);
        lastLine.TaxableAmount = lastLine.LineExtensionAmount;
        lastLine.TaxAmount = amount(lastLine.TaxAmount).add(taxDifference).setScale(2, RoundingMode.HALF_UP);
        if (lastLine.Quantity != null && lastLine.Quantity.compareTo(BigDecimal.ZERO) > 0) {
            lastLine.UnitPrice = lastLine.LineExtensionAmount.divide(lastLine.Quantity, 2, RoundingMode.HALF_UP);
        }
    }

    private String resolveSunatDocumentType(String series) {
        if (series.startsWith("F")) return SUNAT_FACTURA;
        if (series.startsWith("B")) return SUNAT_BOLETA;
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

    private String normalizeSunatUnitCode(String unitCode) {
        if (unitCode == null || unitCode.isBlank()) {
            return "NIU";
        }
        return "NIU".equalsIgnoreCase(unitCode.trim()) ? "NIU" : "BX";
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

    private record DocumentNumber(String series, int correlative) {
    }

    private record SalePayload(
            String sourceModule,
            String sourceDocumentCod,
            String sourceDocumentType,
            String sunatDocumentType,
            String series,
            int correlative,
            Date issueDate,
            String currencyCod,
            String paymentCondition,
            SunatPartyDto supplier,
            SunatPartyDto customer,
            SunatDocumentTotalsDto totals,
            List<SunatDocumentLineDto> lines
    ) {
    }
}
