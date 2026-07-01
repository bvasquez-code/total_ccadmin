package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatRelatedDocumentDto;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class SunatNotaCreditoXmlBuildService extends SunatXmlBuildSupport {

    public String buildXmlNotaCredito(SunatElectronicDocumentDto dto) {
        try {
            Document document = newDocument();
            Element root = createRoot(document, "CreditNote", "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2");
            document.appendChild(root);

            appendUblExtensions(document, root);
            text(document, root, CBC_NS, "cbc:UBLVersionID", "2.1");
            text(document, root, CBC_NS, "cbc:CustomizationID", "2.0");
            text(document, root, CBC_NS, "cbc:ID", SunatDocumentNameUtil.fullDocumentNumber(dto.Series, dto.Correlative));
            text(document, root, CBC_NS, "cbc:IssueDate", formatDate(dto.IssueDate));
            if (dto.IssueTime != null && !dto.IssueTime.isBlank()) {
                text(document, root, CBC_NS, "cbc:IssueTime", dto.IssueTime);
            }
            appendCreditNoteTypeCode(document, root, dto);
            if (dto.Note != null && !dto.Note.isBlank()) {
                text(document, root, CBC_NS, "cbc:Note", dto.Note);
            }
            text(document, root, CBC_NS, "cbc:DocumentCurrencyCode", dto.CurrencyCod);
            appendCreditNoteReferences(document, root, dto);
            appendSupplier(document, root, dto.Supplier);
            appendCustomer(document, root, dto.Customer);
            appendTaxTotal(document, root, dto.Totals, dto.CurrencyCod);
            appendMonetaryTotal(document, root, dto, "cac:LegalMonetaryTotal");
            appendCreditNoteLines(document, root, dto);
            return toXml(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar XML de nota de credito SUNAT: " + ex.getMessage(), ex);
        }
    }

    private void appendCreditNoteTypeCode(Document document, Element root, SunatElectronicDocumentDto dto) {
        Element type = text(document, root, CBC_NS, "cbc:CreditNoteTypeCode", dto.SunatDocumentType);
        type.setAttribute("listAgencyName", "PE:SUNAT");
        type.setAttribute("listName", "Tipo de Documento");
    }

    private void appendCreditNoteReferences(Document document, Element root, SunatElectronicDocumentDto dto) {
        Element discrepancy = element(document, CAC_NS, "cac:DiscrepancyResponse");
        text(document, discrepancy, CBC_NS, "cbc:ReferenceID", dto.DiscrepancyResponse.ReferenceDocumentNumber);
        text(document, discrepancy, CBC_NS, "cbc:ResponseCode", dto.DiscrepancyResponse.ResponseCode);
        text(document, discrepancy, CBC_NS, "cbc:Description", dto.DiscrepancyResponse.Description);
        root.appendChild(discrepancy);

        for (SunatRelatedDocumentDto related : dto.RelatedDocuments) {
            Element billingReference = element(document, CAC_NS, "cac:BillingReference");
            Element invoiceDocumentReference = element(document, CAC_NS, "cac:InvoiceDocumentReference");
            text(document, invoiceDocumentReference, CBC_NS, "cbc:ID", related.DocumentNumber);
            text(document, invoiceDocumentReference, CBC_NS, "cbc:DocumentTypeCode", related.DocumentType);
            billingReference.appendChild(invoiceDocumentReference);
            root.appendChild(billingReference);
        }
    }
}
