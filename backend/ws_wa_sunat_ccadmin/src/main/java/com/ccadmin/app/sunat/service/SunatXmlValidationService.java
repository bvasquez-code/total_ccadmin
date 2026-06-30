package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatDocumentLineDto;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatPartyDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SunatXmlValidationService {

    public void validateForXml(SunatElectronicDocumentDto document) {
        if (document == null)
            throw new IllegalArgumentException("Documento electronico requerido");
        if (document.SourceModule == null || document.SourceModule.isBlank())
            throw new IllegalArgumentException("Modulo origen requerido");
        if (document.SourceDocumentCod == null || document.SourceDocumentCod.isBlank())
            throw new IllegalArgumentException("Documento origen requerido");
        if (!SunatDocumentTypeConst.isValid(document.SunatDocumentType))
            throw new IllegalArgumentException("Tipo de documento SUNAT invalido");
        if (SunatDocumentTypeConst.RESUMEN_DIARIO.equals(document.SunatDocumentType)
                || SunatDocumentTypeConst.COMUNICACION_BAJA.equals(document.SunatDocumentType))
            throw new IllegalArgumentException("Resumen diario y comunicacion de baja se implementan en fases posteriores");
        if (document.Series == null || document.Series.isBlank())
            throw new IllegalArgumentException("Serie requerida");
        if (document.Correlative <= 0)
            throw new IllegalArgumentException("Correlativo requerido");
        if (document.IssueDate == null)
            throw new IllegalArgumentException("Fecha de emision requerida");
        if (document.CurrencyCod == null || document.CurrencyCod.isBlank())
            throw new IllegalArgumentException("Moneda requerida");
        validateSupplier(document.Supplier);
        validateCustomer(document.Customer, document.SunatDocumentType);
        if (document.Totals == null)
            throw new IllegalArgumentException("Totales requeridos");
        if (document.Lines == null || document.Lines.isEmpty())
            throw new IllegalArgumentException("Detalle de documento requerido");
        document.Lines.forEach(this::validateLine);
        validateTotals(document);
        validateNoteDocuments(document);
    }

    private void validateSupplier(SunatPartyDto supplier) {
        if (supplier == null)
            throw new IllegalArgumentException("Datos de emisor requeridos");
        if (!"6".equals(supplier.DocumentType))
            throw new IllegalArgumentException("El emisor debe usar tipo de documento RUC catalogo 6");
        if (supplier.DocumentNumber == null || !supplier.DocumentNumber.matches("^\\d{11}$"))
            throw new IllegalArgumentException("RUC emisor debe tener 11 digitos");
        if (supplier.LegalName == null || supplier.LegalName.isBlank())
            throw new IllegalArgumentException("Razon social del emisor requerida");
    }

    private void validateCustomer(SunatPartyDto customer, String documentType) {
        if (customer == null)
            throw new IllegalArgumentException("Datos de cliente requeridos");
        if (customer.DocumentType == null || customer.DocumentType.isBlank())
            throw new IllegalArgumentException("Tipo de documento del cliente requerido");
        if (customer.DocumentNumber == null || customer.DocumentNumber.isBlank())
            throw new IllegalArgumentException("Numero de documento del cliente requerido");
        if (customer.LegalName == null || customer.LegalName.isBlank())
            throw new IllegalArgumentException("Nombre o razon social del cliente requerido");
        if (SunatDocumentTypeConst.FACTURA.equals(documentType) && !"6".equals(customer.DocumentType))
            throw new IllegalArgumentException("Factura requiere cliente con RUC");
    }

    private void validateLine(SunatDocumentLineDto line) {
        if (line.ItemNumber <= 0)
            throw new IllegalArgumentException("ItemNumber debe ser mayor a cero");
        if (line.Description == null || line.Description.isBlank())
            throw new IllegalArgumentException("Descripcion de item requerida");
        if (line.UnitCode == null || line.UnitCode.isBlank())
            throw new IllegalArgumentException("Unidad de medida requerida");
        if (isNullOrLessEqualZero(line.Quantity))
            throw new IllegalArgumentException("Cantidad debe ser mayor a cero");
        if (isNullOrNegative(line.LineExtensionAmount))
            throw new IllegalArgumentException("Valor de venta de item invalido");
        if (isNullOrNegative(line.TaxAmount))
            throw new IllegalArgumentException("Impuesto de item invalido");
        if (line.TaxCategoryCode == null || line.TaxCategoryCode.isBlank())
            throw new IllegalArgumentException("Categoria de impuesto requerida");
        if (line.TaxExemptionReasonCode == null || line.TaxExemptionReasonCode.isBlank())
            line.TaxExemptionReasonCode = taxExemptionReasonCode(line.TaxCategoryCode);
        if (line.TaxSchemeId == null || line.TaxSchemeId.isBlank())
            line.TaxSchemeId = "1000";
        if (line.TaxSchemeName == null || line.TaxSchemeName.isBlank())
            line.TaxSchemeName = "IGV";
        if (line.TaxTypeCode == null || line.TaxTypeCode.isBlank())
            line.TaxTypeCode = "VAT";
        if (line.PriceTypeCode == null || line.PriceTypeCode.isBlank())
            line.PriceTypeCode = "01";
        if (line.PriceAmount == null)
            line.PriceAmount = line.UnitPrice == null ? BigDecimal.ZERO : line.UnitPrice;
        if (line.TaxableAmount == null)
            line.TaxableAmount = line.LineExtensionAmount;
    }

    private void validateTotals(SunatElectronicDocumentDto document) {
        if (isNullOrNegative(document.Totals.PayableAmount))
            throw new IllegalArgumentException("Importe total invalido");
        if (isNullOrNegative(document.Totals.TaxAmount))
            throw new IllegalArgumentException("Total de impuestos invalido");
        BigDecimal detailTotal = document.Lines.stream()
                .map(line -> line.LineExtensionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (document.Totals.LineExtensionAmount == null || document.Totals.LineExtensionAmount.compareTo(BigDecimal.ZERO) == 0)
            document.Totals.LineExtensionAmount = detailTotal;
        if (detailTotal.setScale(2, RoundingMode.HALF_UP)
                .compareTo(document.Totals.LineExtensionAmount.setScale(2, RoundingMode.HALF_UP)) != 0)
            throw new IllegalArgumentException("Total de lineas no coincide con LineExtensionAmount");
        if (document.Totals.TaxInclusiveAmount == null || document.Totals.TaxInclusiveAmount.compareTo(BigDecimal.ZERO) == 0)
            document.Totals.TaxInclusiveAmount = document.Totals.LineExtensionAmount.add(document.Totals.TaxAmount);
    }

    private void validateNoteDocuments(SunatElectronicDocumentDto document) {
        boolean isNote = SunatDocumentTypeConst.NOTA_CREDITO.equals(document.SunatDocumentType)
                || SunatDocumentTypeConst.NOTA_DEBITO.equals(document.SunatDocumentType);
        if (!isNote)
            return;
        if (document.DiscrepancyResponse == null)
            throw new IllegalArgumentException("Motivo de nota requerido");
        if (document.DiscrepancyResponse.ReferenceDocumentNumber == null || document.DiscrepancyResponse.ReferenceDocumentNumber.isBlank())
            throw new IllegalArgumentException("Documento de referencia requerido para la nota");
        if (document.DiscrepancyResponse.ResponseCode == null || document.DiscrepancyResponse.ResponseCode.isBlank())
            throw new IllegalArgumentException("Codigo de motivo requerido para la nota");
        if (document.DiscrepancyResponse.Description == null || document.DiscrepancyResponse.Description.isBlank())
            throw new IllegalArgumentException("Descripcion de motivo requerida para la nota");
        if (document.RelatedDocuments == null || document.RelatedDocuments.isEmpty())
            throw new IllegalArgumentException("Documento relacionado requerido para la nota");
    }

    private boolean isNullOrNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0;
    }

    private boolean isNullOrLessEqualZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    private String taxExemptionReasonCode(String taxCategoryCode) {
        if ("E".equals(taxCategoryCode)) return "20";
        if ("O".equals(taxCategoryCode)) return "30";
        if ("Z".equals(taxCategoryCode)) return "21";
        return "10";
    }
}
