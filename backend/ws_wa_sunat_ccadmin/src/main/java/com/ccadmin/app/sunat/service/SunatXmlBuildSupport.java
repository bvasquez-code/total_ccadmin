package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatDocumentLineDto;
import com.ccadmin.app.sunat.model.dto.SunatDocumentTotalsDto;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatPartyDto;
import com.ccadmin.app.sunat.model.dto.SunatPaymentTermDto;
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

public abstract class SunatXmlBuildSupport {

    protected static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    protected static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    protected static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    protected Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    protected Element createRoot(Document document, String rootName, String rootNamespace) {
        Element root = document.createElementNS(rootNamespace, rootName);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
        return root;
    }

    protected void appendUblExtensions(Document document, Element root) {
        Element extensions = element(document, EXT_NS, "ext:UBLExtensions");
        Element extension = element(document, EXT_NS, "ext:UBLExtension");
        Element content = element(document, EXT_NS, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
    }

    protected void appendSupplier(Document document, Element root, SunatPartyDto supplier) {
        Element accountingSupplierParty = element(document, CAC_NS, "cac:AccountingSupplierParty");
        appendParty(document, accountingSupplierParty, supplier, true);
        root.appendChild(accountingSupplierParty);
    }

    protected void appendCustomer(Document document, Element root, SunatPartyDto customer) {
        Element accountingCustomerParty = element(document, CAC_NS, "cac:AccountingCustomerParty");
        appendParty(document, accountingCustomerParty, customer, false);
        root.appendChild(accountingCustomerParty);
    }

    protected void appendDespatchSupplier(Document document, Element root, SunatPartyDto supplier) {
        Element partyElement = element(document, CAC_NS, "cac:DespatchSupplierParty");
        appendParty(document, partyElement, supplier, true);
        root.appendChild(partyElement);
    }

    protected void appendDeliveryCustomer(Document document, Element root, SunatPartyDto customer) {
        Element partyElement = element(document, CAC_NS, "cac:DeliveryCustomerParty");
        appendParty(document, partyElement, customer, false);
        root.appendChild(partyElement);
    }

    protected void appendPaymentTerms(Document document, Element root, SunatElectronicDocumentDto dto) {
        String condition = dto.PaymentCondition == null || dto.PaymentCondition.isBlank() ? "Contado" : dto.PaymentCondition.trim();
        Element paymentTerm = element(document, CAC_NS, "cac:PaymentTerms");
        text(document, paymentTerm, CBC_NS, "cbc:ID", "FormaPago");
        text(document, paymentTerm, CBC_NS, "cbc:PaymentMeansID", condition);
        if ("Credito".equalsIgnoreCase(condition)) {
            money(document, paymentTerm, "cbc:Amount", dto.Totals == null ? BigDecimal.ZERO : dto.Totals.PayableAmount, dto.CurrencyCod);
        }
        root.appendChild(paymentTerm);

        if (!"Credito".equalsIgnoreCase(condition) || dto.PaymentTerms == null) {
            return;
        }

        int number = 1;
        for (SunatPaymentTermDto term : dto.PaymentTerms) {
            Element installment = element(document, CAC_NS, "cac:PaymentTerms");
            text(document, installment, CBC_NS, "cbc:ID", "FormaPago");
            text(document, installment, CBC_NS, "cbc:PaymentMeansID", term.PaymentMeansId == null || term.PaymentMeansId.isBlank()
                    ? String.format("Cuota%03d", number)
                    : term.PaymentMeansId.trim());
            money(document, installment, "cbc:Amount", term.Amount, dto.CurrencyCod);
            text(document, installment, CBC_NS, "cbc:PaymentDueDate", formatDate(term.PaymentDueDate));
            root.appendChild(installment);
            number++;
        }
    }

    protected void appendTaxTotal(Document document, Element root, SunatDocumentTotalsDto totals, String currency) {
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

    protected void appendMonetaryTotal(Document document, Element root, SunatElectronicDocumentDto dto, String elementName) {
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

    protected void appendInvoiceLines(Document document, Element root, SunatElectronicDocumentDto dto) {
        appendDocumentLines(document, root, dto, "cac:InvoiceLine", "cbc:InvoicedQuantity");
    }

    protected void appendCreditNoteLines(Document document, Element root, SunatElectronicDocumentDto dto) {
        appendDocumentLines(document, root, dto, "cac:CreditNoteLine", "cbc:CreditedQuantity");
    }

    protected void appendDebitNoteLines(Document document, Element root, SunatElectronicDocumentDto dto) {
        appendDocumentLines(document, root, dto, "cac:DebitNoteLine", "cbc:DebitedQuantity");
    }

    protected void appendItem(Document document, Element lineElement, SunatDocumentLineDto line) {
        Element item = element(document, CAC_NS, "cac:Item");
        text(document, item, CBC_NS, "cbc:Description", line.Description);
        if (line.ProductCode != null && !line.ProductCode.isBlank()) {
            Element seller = element(document, CAC_NS, "cac:SellersItemIdentification");
            text(document, seller, CBC_NS, "cbc:ID", line.ProductCode);
            item.appendChild(seller);
        }
        lineElement.appendChild(item);
    }

    protected Element element(Document document, String namespace, String name) {
        return document.createElementNS(namespace, name);
    }

    protected Element text(Document document, Element parent, String namespace, String name, String value) {
        Element child = element(document, namespace, name);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
        return child;
    }

    protected Element money(Document document, Element parent, String name, BigDecimal value, String currency) {
        Element child = text(document, parent, CBC_NS, name, amount(value));
        child.setAttribute("currencyID", currency);
        return child;
    }

    protected String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    protected String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    protected String amount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected String amount3(BigDecimal value) {
        if (value == null) {
            return "0.000";
        }
        return value.setScale(3, RoundingMode.HALF_UP).toPlainString();
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

        if (!supplier) {
            appendAddress(document, party, partyDto, "cac:PostalAddress", false);
        }

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
        if (supplier) {
            appendAddress(document, partyLegalEntity, partyDto, "cac:RegistrationAddress", true);
        }
        party.appendChild(partyLegalEntity);

        parent.appendChild(party);
    }

    private void appendAddress(Document document, Element parent, SunatPartyDto partyDto, String elementName, boolean supplier) {
        if (partyDto.Address == null || partyDto.Address.isBlank()) {
            return;
        }
        Element address = element(document, CAC_NS, elementName);
        if (partyDto.UbigeoCod != null && !partyDto.UbigeoCod.isBlank()) {
            Element ubigeo = text(document, address, CBC_NS, "cbc:ID", partyDto.UbigeoCod);
            ubigeo.setAttribute("schemeAgencyName", "PE:INEI");
            ubigeo.setAttribute("schemeName", "Ubigeos");
        }
        if (supplier) {
            Element addressTypeCode = text(document, address, CBC_NS, "cbc:AddressTypeCode", normalizeAddressTypeCode(partyDto.AddressTypeCode));
            addressTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
            addressTypeCode.setAttribute("listName", "Establecimientos anexos");
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
        parent.appendChild(address);
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

    private void appendDocumentLines(Document document, Element root, SunatElectronicDocumentDto dto,
                                     String lineElementName, String quantityElementName) {
        for (SunatDocumentLineDto line : dto.Lines) {
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

    private void appendPrice(Document document, Element lineElement, SunatDocumentLineDto line, String currency) {
        Element price = element(document, CAC_NS, "cac:Price");
        money(document, price, "cbc:PriceAmount", line.UnitPrice == null ? BigDecimal.ZERO : line.UnitPrice, currency);
        lineElement.appendChild(price);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String normalizeAddressTypeCode(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        String code = value.trim();
        if (!code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("AddressTypeCode del emisor debe tener 4 digitos");
        }
        return code;
    }
}
