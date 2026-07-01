package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class SunatFacturaXmlBuildService extends SunatXmlBuildSupport {

    public String buildXmlFactura(SunatElectronicDocumentDto dto) {
        try {
            Document document = newDocument();
            Element root = createRoot(document, "Invoice", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
            document.appendChild(root);

            appendUblExtensions(document, root);
            text(document, root, CBC_NS, "cbc:UBLVersionID", "2.1");
            text(document, root, CBC_NS, "cbc:CustomizationID", "2.0");
            text(document, root, CBC_NS, "cbc:ID", SunatDocumentNameUtil.fullDocumentNumber(dto.Series, dto.Correlative));
            text(document, root, CBC_NS, "cbc:IssueDate", formatDate(dto.IssueDate));
            if (dto.IssueTime != null && !dto.IssueTime.isBlank()) {
                text(document, root, CBC_NS, "cbc:IssueTime", dto.IssueTime);
            }
            appendInvoiceTypeCode(document, root, dto);
            if (dto.Note != null && !dto.Note.isBlank()) {
                text(document, root, CBC_NS, "cbc:Note", dto.Note);
            }
            text(document, root, CBC_NS, "cbc:DocumentCurrencyCode", dto.CurrencyCod);
            appendSupplier(document, root, dto.Supplier);
            appendCustomer(document, root, dto.Customer);
            appendPaymentTerms(document, root, dto);
            appendTaxTotal(document, root, dto.Totals, dto.CurrencyCod);
            appendMonetaryTotal(document, root, dto, "cac:LegalMonetaryTotal");
            appendInvoiceLines(document, root, dto);
            return toXml(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar XML de factura SUNAT: " + ex.getMessage(), ex);
        }
    }

    private void appendInvoiceTypeCode(Document document, Element root, SunatElectronicDocumentDto dto) {
        Element type = text(document, root, CBC_NS, "cbc:InvoiceTypeCode", dto.SunatDocumentType);
        type.setAttribute("listID", dto.OperationTypeCode == null || dto.OperationTypeCode.isBlank() ? "0101" : dto.OperationTypeCode);
        type.setAttribute("listAgencyName", "PE:SUNAT");
        type.setAttribute("listName", "Tipo de Documento");
    }
}
