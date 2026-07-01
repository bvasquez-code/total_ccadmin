package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatSoapResponseDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class SunatSoapClientService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public SunatSoapResponseDto sendBill(SunatConfigEntity config, String fileName, byte[] zipContent) {
        String endpoint = config.InvoiceEndpoint;
        String body = envelope(config, """
                <ser:sendBill>
                    <fileName>%s</fileName>
                    <contentFile>%s</contentFile>
                </ser:sendBill>
                """.formatted(escape(fileName), Base64.getEncoder().encodeToString(zipContent)));
        return post(endpoint, body);
    }

    public SunatSoapResponseDto sendSummary(SunatConfigEntity config, String fileName, byte[] zipContent) {
        String endpoint = config.SummaryEndpoint;
        String body = envelope(config, """
                <ser:sendSummary>
                    <fileName>%s</fileName>
                    <contentFile>%s</contentFile>
                </ser:sendSummary>
                """.formatted(escape(fileName), Base64.getEncoder().encodeToString(zipContent)));
        return post(endpoint, body);
    }

    public SunatSoapResponseDto sendGuide(SunatConfigEntity config, String fileName, byte[] zipContent) {
        String endpoint = config.GuideEndpoint;
        String body = envelope(config, """
                <ser:sendBill>
                    <fileName>%s</fileName>
                    <contentFile>%s</contentFile>
                </ser:sendBill>
                """.formatted(escape(fileName), Base64.getEncoder().encodeToString(zipContent)));
        return post(endpoint, body);
    }

    public SunatSoapResponseDto getStatus(SunatConfigEntity config, String ticket) {
        String endpoint = config.TicketEndpoint;
        String body = envelope(config, """
                <ser:getStatus>
                    <ticket>%s</ticket>
                </ser:getStatus>
                """.formatted(escape(ticket)));
        return post(endpoint, body);
    }

    private SunatSoapResponseDto post(String endpoint, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "text/xml; charset=\"utf-8\"")
                    .header("Accept", "text/xml")
                    .header("SOAPAction", "")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parse(endpoint, response.statusCode(), response.body());
        } catch (Exception ex) {
            SunatSoapResponseDto dto = new SunatSoapResponseDto();
            dto.Endpoint = endpoint;
            dto.HttpStatusCode = 0;
            dto.FaultString = ex.getMessage();
            dto.RawResponse = ex.toString();
            return dto;
        }
    }

    private String envelope(SunatConfigEntity config, String operationBody) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:ser="http://service.sunat.gob.pe"
                                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                    <soapenv:Header>
                        <wsse:Security>
                            <wsse:UsernameToken>
                                <wsse:Username>%s</wsse:Username>
                                <wsse:Password>%s</wsse:Password>
                            </wsse:UsernameToken>
                        </wsse:Security>
                    </soapenv:Header>
                    <soapenv:Body>
                        %s
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                escape(config.IssuerRuc + config.SolUser),
                escape(config.SolPassword),
                operationBody
        );
    }

    private SunatSoapResponseDto parse(String endpoint, int statusCode, String rawResponse) {
        SunatSoapResponseDto dto = new SunatSoapResponseDto();
        dto.Endpoint = endpoint;
        dto.HttpStatusCode = statusCode;
        dto.RawResponse = rawResponse;
        if (rawResponse == null || rawResponse.isBlank()) {
            return dto;
        }
        try {
            Document document = parseXml(rawResponse);
            dto.FaultCode = firstText(document, "faultcode");
            dto.FaultString = firstText(document, "faultstring");
            dto.ApplicationResponseBase64 = firstText(document, "applicationResponse");
            dto.Ticket = firstText(document, "ticket");
            dto.ContentBase64 = firstText(document, "content");
            return dto;
        } catch (Exception ex) {
            dto.FaultString = "Respuesta SOAP no pudo ser interpretada: " + ex.getMessage();
            return dto;
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String firstText(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(localName);
        }
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
