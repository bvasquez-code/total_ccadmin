package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatDocumentLineDto;
import com.ccadmin.app.sunat.model.dto.SunatDocumentTotalsDto;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatPartyDto;
import com.ccadmin.app.sunat.model.dto.SunatRelatedDocumentDto;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class SunatUblXmlBuildService {

    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    public String build(SunatElectronicDocumentDto dto) {
        try {
            Document document = newDocument();
            Element root = createRoot(document, dto.SunatDocumentType);
            document.appendChild(root);

            appendUblExtensions(document, root);
            text(document, root, CBC_NS, "cbc:UBLVersionID", "2.1");
            text(document, root, CBC_NS, "cbc:CustomizationID", "2.0");
            text(document, root, CBC_NS, "cbc:ID", SunatDocumentNameUtil.fullDocumentNumber(dto.Series, dto.Correlative));
            text(document, root, CBC_NS, "cbc:IssueDate", formatDate(dto.IssueDate));
            if (dto.IssueTime != null && !dto.IssueTime.isBlank()) {
                text(document, root, CBC_NS, "cbc:IssueTime", dto.IssueTime);
            }
            appendDocumentType(document, root, dto);
            if (dto.Note != null && !dto.Note.isBlank()) {
                text(document, root, CBC_NS, "cbc:Note", dto.Note);
            }
            text(document, root, CBC_NS, "cbc:DocumentCurrencyCode", dto.CurrencyCod);

            appendNoteReferences(document, root, dto);
            appendSupplier(document, root, dto.Supplier);
            appendCustomer(document, root, dto.Customer);
            appendTaxTotal(document, root, dto.Totals, dto.CurrencyCod);
            appendMonetaryTotal(document, root, dto);
            appendLines(document, root, dto);

            return toXml(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar XML UBL 2.1: " + ex.getMessage(), ex);
        }
    }

    private Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    private Element createRoot(Document document, String sunatDocumentType) {
        String rootName = "Invoice";
        String rootNamespace = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
        if (SunatDocumentTypeConst.NOTA_CREDITO.equals(sunatDocumentType)) {
            rootName = "CreditNote";
            rootNamespace = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
        } else if (SunatDocumentTypeConst.NOTA_DEBITO.equals(sunatDocumentType)) {
            rootName = "DebitNote";
            rootNamespace = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";
        }
        Element root = document.createElementNS(rootNamespace, rootName);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
        return root;
    }

    private void appendUblExtensions(Document document, Element root) {
        Element extensions = element(document, EXT_NS, "ext:UBLExtensions");
        Element extension = element(document, EXT_NS, "ext:UBLExtension");
        Element content = element(document, EXT_NS, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
    }

    private void appendDocumentType(Document document, Element root, SunatElectronicDocumentDto dto) {
        if (SunatDocumentTypeConst.FACTURA.equals(dto.SunatDocumentType) || SunatDocumentTypeConst.BOLETA.equals(dto.SunatDocumentType)) {
            Element type = text(document, root, CBC_NS, "cbc:InvoiceTypeCode", dto.SunatDocumentType);
            type.setAttribute("listID", dto.OperationTypeCode == null || dto.OperationTypeCode.isBlank() ? "0101" : dto.OperationTypeCode);
            type.setAttribute("listAgencyName", "PE:SUNAT");
            type.setAttribute("listName", "Tipo de Documento");
            return;
        }
        if (SunatDocumentTypeConst.NOTA_CREDITO.equals(dto.SunatDocumentType)) {
            Element type = text(document, root, CBC_NS, "cbc:CreditNoteTypeCode", dto.SunatDocumentType);
            type.setAttribute("listAgencyName", "PE:SUNAT");
            type.setAttribute("listName", "Tipo de Documento");
            return;
        }
        if (SunatDocumentTypeConst.NOTA_DEBITO.equals(dto.SunatDocumentType)) {
            Element type = text(document, root, CBC_NS, "cbc:DebitNoteTypeCode", dto.SunatDocumentType);
            type.setAttribute("listAgencyName", "PE:SUNAT");
            type.setAttribute("listName", "Tipo de Documento");
        }
    }

    private void appendNoteReferences(Document document, Element root, SunatElectronicDocumentDto dto) {
        boolean isCreditNote = SunatDocumentTypeConst.NOTA_CREDITO.equals(dto.SunatDocumentType);
        boolean isDebitNote = SunatDocumentTypeConst.NOTA_DEBITO.equals(dto.SunatDocumentType);
        if (!isCreditNote && !isDebitNote) {
            return;
        }

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

    private void appendSupplier(Document document, Element root, SunatPartyDto supplier) {
        Element accountingSupplierParty = element(document, CAC_NS, "cac:AccountingSupplierParty");
        appendParty(document, accountingSupplierParty, supplier, true);
        root.appendChild(accountingSupplierParty);
    }

    private void appendCustomer(Document document, Element root, SunatPartyDto customer) {
        Element accountingCustomerParty = element(document, CAC_NS, "cac:AccountingCustomerParty");
        appendParty(document, accountingCustomerParty, customer, false);
        root.appendChild(accountingCustomerParty);
    }

    private void appendParty(Document document, Element parent, SunatPartyDto partyDto, boolean supplier) {
        Element party = element(document, CAC_NS, "cac:Party");

        Element partyIdentification = element(document, CAC_NS, "cac:PartyIdentification");
        Element id = text(document, partyIdentification, CBC_NS, "cbc:ID", partyDto.DocumentNumber);
        id.setAttribute("schemeID", partyDto.DocumentType);
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        party.appendChild(partyIdentification);

        String tradeName = partyDto.TradeName == null || partyDto.TradeName.isBlank() ? partyDto.LegalName : partyDto.TradeName;
        Element partyName = element(document, CAC_NS, "cac:PartyName");
        text(document, partyName, CBC_NS, "cbc:Name", tradeName);
        party.appendChild(partyName);

        appendAddress(document, party, partyDto, supplier);

        Element partyTaxScheme = element(document, CAC_NS, "cac:PartyTaxScheme");
        text(document, partyTaxScheme, CBC_NS, "cbc:RegistrationName", partyDto.LegalName);
        Element companyId = text(document, partyTaxScheme, CBC_NS, "cbc:CompanyID", partyDto.DocumentNumber);
        companyId.setAttribute("schemeID", partyDto.DocumentType);
        companyId.setAttribute("schemeName", "Documento de Identidad");
        companyId.setAttribute("schemeAgencyName", "PE:SUNAT");
        Element taxScheme = element(document, CAC_NS, "cac:TaxScheme");
        text(document, taxScheme, CBC_NS, "cbc:ID", "1000");
        text(document, taxScheme, CBC_NS, "cbc:Name", "IGV");
        text(document, taxScheme, CBC_NS, "cbc:TaxTypeCode", "VAT");
        partyTaxScheme.appendChild(taxScheme);
        party.appendChild(partyTaxScheme);

        Element partyLegalEntity = element(document, CAC_NS, "cac:PartyLegalEntity");
        text(document, partyLegalEntity, CBC_NS, "cbc:RegistrationName", partyDto.LegalName);
        party.appendChild(partyLegalEntity);

        parent.appendChild(party);
    }

    private void appendAddress(Document document, Element party, SunatPartyDto partyDto, boolean supplier) {
        if (partyDto.Address == null || partyDto.Address.isBlank()) {
            return;
        }
        Element address = element(document, CAC_NS, supplier ? "cac:PostalAddress" : "cac:PostalAddress");
        if (partyDto.UbigeoCod != null && !partyDto.UbigeoCod.isBlank()) {
            text(document, address, CBC_NS, "cbc:ID", partyDto.UbigeoCod);
        }
        text(document, address, CBC_NS, "cbc:StreetName", partyDto.Address);
        if (partyDto.District != null && !partyDto.District.isBlank()) {
            text(document, address, CBC_NS, "cbc:CitySubdivisionName", partyDto.District);
        }
        if (partyDto.Province != null && !partyDto.Province.isBlank()) {
            text(document, address, CBC_NS, "cbc:CityName", partyDto.Province);
        }
        if (partyDto.Department != null && !partyDto.Department.isBlank()) {
            text(document, address, CBC_NS, "cbc:CountrySubentity", partyDto.Department);
        }
        Element country = element(document, CAC_NS, "cac:Country");
        text(document, country, CBC_NS, "cbc:IdentificationCode", partyDto.CountryCode == null ? "PE" : partyDto.CountryCode);
        address.appendChild(country);
        party.appendChild(address);
    }

    private void appendTaxTotal(Document document, Element root, SunatDocumentTotalsDto totals, String currency) {
        Element taxTotal = element(document, CAC_NS, "cac:TaxTotal");
        money(document, taxTotal, "cbc:TaxAmount", totals.TaxAmount, currency);
        boolean hasSubtotal = false;
        if (isPositive(totals.TaxableAmount) || isPositive(totals.TaxAmount)) {
            appendTaxSubtotal(document, taxTotal, totals.TaxableAmount, totals.TaxAmount, "S", "1000", "IGV", "VAT", currency);
            hasSubtotal = true;
        }
        if (isPositive(totals.ExoneratedAmount)) {
            appendTaxSubtotal(document, taxTotal, totals.ExoneratedAmount, BigDecimal.ZERO, "E", "9997", "EXO", "VAT", currency);
            hasSubtotal = true;
        }
        if (isPositive(totals.UnaffectedAmount)) {
            appendTaxSubtotal(document, taxTotal, totals.UnaffectedAmount, BigDecimal.ZERO, "O", "9998", "INA", "FRE", currency);
            hasSubtotal = true;
        }
        if (isPositive(totals.FreeAmount)) {
            appendTaxSubtotal(document, taxTotal, totals.FreeAmount, BigDecimal.ZERO, "Z", "9996", "GRA", "FRE", currency);
            hasSubtotal = true;
        }
        if (!hasSubtotal) {
            appendTaxSubtotal(document, taxTotal, BigDecimal.ZERO, BigDecimal.ZERO, "S", "1000", "IGV", "VAT", currency);
        }
        root.appendChild(taxTotal);
    }

    private void appendTaxSubtotal(Document document, Element taxTotal, BigDecimal taxable, BigDecimal tax, String categoryCode,
                                   String taxSchemeId, String taxSchemeName, String taxTypeCode, String currency) {
        Element subtotal = element(document, CAC_NS, "cac:TaxSubtotal");
        money(document, subtotal, "cbc:TaxableAmount", taxable, currency);
        money(document, subtotal, "cbc:TaxAmount", tax, currency);
        Element category = element(document, CAC_NS, "cac:TaxCategory");
        text(document, category, CBC_NS, "cbc:ID", categoryCode);
        Element scheme = element(document, CAC_NS, "cac:TaxScheme");
        text(document, scheme, CBC_NS, "cbc:ID", taxSchemeId);
        text(document, scheme, CBC_NS, "cbc:Name", taxSchemeName);
        text(document, scheme, CBC_NS, "cbc:TaxTypeCode", taxTypeCode);
        category.appendChild(scheme);
        subtotal.appendChild(category);
        taxTotal.appendChild(subtotal);
    }

    private void appendMonetaryTotal(Document document, Element root, SunatElectronicDocumentDto dto) {
        String elementName = SunatDocumentTypeConst.NOTA_DEBITO.equals(dto.SunatDocumentType)
                ? "cac:RequestedMonetaryTotal"
                : "cac:LegalMonetaryTotal";
        Element total = element(document, CAC_NS, elementName);
        money(document, total, "cbc:LineExtensionAmount", dto.Totals.LineExtensionAmount, dto.CurrencyCod);
        money(document, total, "cbc:TaxInclusiveAmount", dto.Totals.TaxInclusiveAmount, dto.CurrencyCod);
        if (isPositive(dto.Totals.DiscountTotal)) {
            money(document, total, "cbc:AllowanceTotalAmount", dto.Totals.DiscountTotal, dto.CurrencyCod);
        }
        if (isPositive(dto.Totals.ChargeTotal)) {
            money(document, total, "cbc:ChargeTotalAmount", dto.Totals.ChargeTotal, dto.CurrencyCod);
        }
        money(document, total, "cbc:PayableAmount", dto.Totals.PayableAmount, dto.CurrencyCod);
        root.appendChild(total);
    }

    private void appendLines(Document document, Element root, SunatElectronicDocumentDto dto) {
        for (SunatDocumentLineDto line : dto.Lines) {
            String lineElementName = "cac:InvoiceLine";
            String quantityElementName = "cbc:InvoicedQuantity";
            if (SunatDocumentTypeConst.NOTA_CREDITO.equals(dto.SunatDocumentType)) {
                lineElementName = "cac:CreditNoteLine";
                quantityElementName = "cbc:CreditedQuantity";
            } else if (SunatDocumentTypeConst.NOTA_DEBITO.equals(dto.SunatDocumentType)) {
                lineElementName = "cac:DebitNoteLine";
                quantityElementName = "cbc:DebitedQuantity";
            }

            Element lineElement = element(document, CAC_NS, lineElementName);
            text(document, lineElement, CBC_NS, "cbc:ID", String.valueOf(line.ItemNumber));
            Element quantity = text(document, lineElement, CBC_NS, quantityElementName, amount(line.Quantity));
            quantity.setAttribute("unitCode", line.UnitCode);
            money(document, lineElement, "cbc:LineExtensionAmount", line.LineExtensionAmount, dto.CurrencyCod);
            appendPricingReference(document, lineElement, line, dto.CurrencyCod);
            appendLineTaxTotal(document, lineElement, line, dto.CurrencyCod);
            appendItem(document, lineElement, line);
            appendPrice(document, lineElement, line, dto.CurrencyCod);
            root.appendChild(lineElement);
        }
    }

    private void appendPricingReference(Document document, Element lineElement, SunatDocumentLineDto line, String currency) {
        Element pricingReference = element(document, CAC_NS, "cac:PricingReference");
        Element alternativePrice = element(document, CAC_NS, "cac:AlternativeConditionPrice");
        money(document, alternativePrice, "cbc:PriceAmount", line.PriceAmount, currency);
        text(document, alternativePrice, CBC_NS, "cbc:PriceTypeCode", line.PriceTypeCode);
        pricingReference.appendChild(alternativePrice);
        lineElement.appendChild(pricingReference);
    }

    private void appendLineTaxTotal(Document document, Element lineElement, SunatDocumentLineDto line, String currency) {
        Element taxTotal = element(document, CAC_NS, "cac:TaxTotal");
        money(document, taxTotal, "cbc:TaxAmount", line.TaxAmount, currency);
        Element subtotal = element(document, CAC_NS, "cac:TaxSubtotal");
        money(document, subtotal, "cbc:TaxableAmount", line.TaxableAmount, currency);
        money(document, subtotal, "cbc:TaxAmount", line.TaxAmount, currency);
        Element category = element(document, CAC_NS, "cac:TaxCategory");
        text(document, category, CBC_NS, "cbc:ID", line.TaxCategoryCode);
        text(document, category, CBC_NS, "cbc:Percent", amount(line.TaxPercent));
        text(document, category, CBC_NS, "cbc:TaxExemptionReasonCode", line.TaxExemptionReasonCode);
        Element scheme = element(document, CAC_NS, "cac:TaxScheme");
        text(document, scheme, CBC_NS, "cbc:ID", line.TaxSchemeId);
        text(document, scheme, CBC_NS, "cbc:Name", line.TaxSchemeName);
        text(document, scheme, CBC_NS, "cbc:TaxTypeCode", line.TaxTypeCode);
        category.appendChild(scheme);
        subtotal.appendChild(category);
        taxTotal.appendChild(subtotal);
        lineElement.appendChild(taxTotal);
    }

    private void appendItem(Document document, Element lineElement, SunatDocumentLineDto line) {
        Element item = element(document, CAC_NS, "cac:Item");
        text(document, item, CBC_NS, "cbc:Description", line.Description);
        if (line.ProductCode != null && !line.ProductCode.isBlank()) {
            Element seller = element(document, CAC_NS, "cac:SellersItemIdentification");
            text(document, seller, CBC_NS, "cbc:ID", line.ProductCode);
            item.appendChild(seller);
        }
        lineElement.appendChild(item);
    }

    private void appendPrice(Document document, Element lineElement, SunatDocumentLineDto line, String currency) {
        Element price = element(document, CAC_NS, "cac:Price");
        money(document, price, "cbc:PriceAmount", line.UnitPrice == null ? BigDecimal.ZERO : line.UnitPrice, currency);
        lineElement.appendChild(price);
    }

    private Element element(Document document, String namespace, String name) {
        return document.createElementNS(namespace, name);
    }

    private Element text(Document document, Element parent, String namespace, String name, String value) {
        Element child = element(document, namespace, name);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
        return child;
    }

    private Element money(Document document, Element parent, String name, BigDecimal value, String currency) {
        Element child = text(document, parent, CBC_NS, name, amount(value));
        child.setAttribute("currencyID", currency);
        return child;
    }

    private String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String amount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
