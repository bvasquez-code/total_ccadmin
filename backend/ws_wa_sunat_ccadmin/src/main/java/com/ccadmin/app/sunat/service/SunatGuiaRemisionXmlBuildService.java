package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatDocumentLineDto;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class SunatGuiaRemisionXmlBuildService extends SunatXmlBuildSupport {

    public String buildXmlGuiaRemision(SunatElectronicDocumentDto dto) {
        try {
            Document document = newDocument();
            Element root = createRoot(document, "DespatchAdvice", "urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2");
            document.appendChild(root);

            appendUblExtensions(document, root);
            text(document, root, CBC_NS, "cbc:UBLVersionID", "2.1");
            text(document, root, CBC_NS, "cbc:CustomizationID", "1.0");
            text(document, root, CBC_NS, "cbc:ID", SunatDocumentNameUtil.fullDocumentNumber(dto.Series, dto.Correlative));
            text(document, root, CBC_NS, "cbc:IssueDate", formatDate(dto.IssueDate));
            if (dto.IssueTime != null && !dto.IssueTime.isBlank()) {
                text(document, root, CBC_NS, "cbc:IssueTime", dto.IssueTime);
            }
            appendDespatchAdviceTypeCode(document, root, dto);
            if (dto.Note != null && !dto.Note.isBlank()) {
                text(document, root, CBC_NS, "cbc:Note", dto.Note);
            }
            appendDespatchSupplier(document, root, dto.Supplier);
            appendDeliveryCustomer(document, root, dto.Customer);
            appendShipment(document, root, dto);
            appendDespatchLines(document, root, dto);
            return toXml(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar XML de guia de remision SUNAT: " + ex.getMessage(), ex);
        }
    }

    private void appendDespatchAdviceTypeCode(Document document, Element root, SunatElectronicDocumentDto dto) {
        Element type = text(document, root, CBC_NS, "cbc:DespatchAdviceTypeCode", dto.SunatDocumentType);
        type.setAttribute("listAgencyName", "PE:SUNAT");
        type.setAttribute("listName", "Tipo de Documento");
    }

    private void appendShipment(Document document, Element root, SunatElectronicDocumentDto dto) {
        Element shipment = element(document, CAC_NS, "cac:Shipment");
        text(document, shipment, CBC_NS, "cbc:ID", "1");
        text(document, shipment, CBC_NS, "cbc:HandlingCode", dto.ReasonTransferCode);
        text(document, shipment, CBC_NS, "cbc:Information", dto.ReasonTransferDescription);
        Element grossWeight = text(document, shipment, CBC_NS, "cbc:GrossWeightMeasure", amount3(dto.TotalWeightKg));
        grossWeight.setAttribute("unitCode", "KGM");
        text(document, shipment, CBC_NS, "cbc:TotalTransportHandlingUnitQuantity", String.valueOf(dto.NumPackages));

        Element shipmentStage = element(document, CAC_NS, "cac:ShipmentStage");
        text(document, shipmentStage, CBC_NS, "cbc:TransportModeCode", dto.TransportModeCode);
        if ("01".equals(dto.TransportModeCode)) {
            appendCarrierParty(document, shipmentStage, dto);
        }
        if ("02".equals(dto.TransportModeCode)) {
            appendPrivateTransport(document, shipmentStage, dto);
        }
        shipment.appendChild(shipmentStage);

        Element delivery = element(document, CAC_NS, "cac:Delivery");
        appendSimpleAddress(document, delivery, "cac:DeliveryAddress", dto.ArrivalUbigeo, dto.ArrivalAddress);
        Element despatch = element(document, CAC_NS, "cac:Despatch");
        appendSimpleAddress(document, despatch, "cac:DespatchAddress", dto.DepartureUbigeo, dto.DepartureAddress);
        delivery.appendChild(despatch);
        shipment.appendChild(delivery);

        root.appendChild(shipment);
    }

    private void appendCarrierParty(Document document, Element shipmentStage, SunatElectronicDocumentDto dto) {
        Element carrierParty = element(document, CAC_NS, "cac:CarrierParty");
        Element partyIdentification = element(document, CAC_NS, "cac:PartyIdentification");
        Element id = text(document, partyIdentification, CBC_NS, "cbc:ID", dto.CarrierRuc);
        id.setAttribute("schemeID", "6");
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        carrierParty.appendChild(partyIdentification);
        Element partyLegalEntity = element(document, CAC_NS, "cac:PartyLegalEntity");
        text(document, partyLegalEntity, CBC_NS, "cbc:RegistrationName", dto.CarrierName);
        carrierParty.appendChild(partyLegalEntity);
        shipmentStage.appendChild(carrierParty);
    }

    private void appendPrivateTransport(Document document, Element shipmentStage, SunatElectronicDocumentDto dto) {
        Element transportMeans = element(document, CAC_NS, "cac:TransportMeans");
        Element roadTransport = element(document, CAC_NS, "cac:RoadTransport");
        text(document, roadTransport, CBC_NS, "cbc:LicensePlateID", dto.VehiclePlate);
        transportMeans.appendChild(roadTransport);
        shipmentStage.appendChild(transportMeans);
        appendDriverPerson(document, shipmentStage, dto);
    }

    private void appendDriverPerson(Document document, Element shipmentStage, SunatElectronicDocumentDto dto) {
        Element driverPerson = element(document, CAC_NS, "cac:DriverPerson");
        Element id = text(document, driverPerson, CBC_NS, "cbc:ID", dto.DriverDocNumber);
        id.setAttribute("schemeID", dto.DriverDocType);
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        text(document, driverPerson, CBC_NS, "cbc:FirstName", "CONDUCTOR");
        Element license = element(document, CAC_NS, "cac:IdentityDocumentReference");
        text(document, license, CBC_NS, "cbc:ID", dto.DriverLicenseNumber);
        driverPerson.appendChild(license);
        shipmentStage.appendChild(driverPerson);
    }

    private void appendSimpleAddress(Document document, Element parent, String elementName, String ubigeo, String addressValue) {
        Element address = element(document, CAC_NS, elementName);
        Element id = text(document, address, CBC_NS, "cbc:ID", ubigeo);
        id.setAttribute("schemeAgencyName", "PE:INEI");
        id.setAttribute("schemeName", "Ubigeos");
        text(document, address, CBC_NS, "cbc:StreetName", addressValue);
        Element country = element(document, CAC_NS, "cac:Country");
        text(document, country, CBC_NS, "cbc:IdentificationCode", "PE");
        address.appendChild(country);
        parent.appendChild(address);
    }

    private void appendDespatchLines(Document document, Element root, SunatElectronicDocumentDto dto) {
        for (SunatDocumentLineDto line : dto.Lines) {
            Element lineElement = element(document, CAC_NS, "cac:DespatchLine");
            text(document, lineElement, CBC_NS, "cbc:ID", String.valueOf(line.ItemNumber));
            Element quantity = text(document, lineElement, CBC_NS, "cbc:DeliveredQuantity", amount(line.Quantity));
            quantity.setAttribute("unitCode", line.UnitCode);
            Element orderLineReference = element(document, CAC_NS, "cac:OrderLineReference");
            text(document, orderLineReference, CBC_NS, "cbc:LineID", String.valueOf(line.ItemNumber));
            lineElement.appendChild(orderLineReference);
            appendItem(document, lineElement, line);
            root.appendChild(lineElement);
        }
    }
}
