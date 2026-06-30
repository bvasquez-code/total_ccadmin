package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatWsResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class SaleSunatClientService {

    @Value("${sunat.service.base-url:http://localhost:8092}")
    private String sunatBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateXml(SunatElectronicDocumentDto request) {
        String url = sunatBaseUrl + "/api/v1/sunat/document/generateXml";
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "generateXml");
        return extractSunatDocumentCod(response.getBody());
    }

    public SunatWsResponseDto process(SunatElectronicDocumentDto request) {
        String url = sunatBaseUrl + "/api/v1/sunat/document/process";
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

    public void signXml(String sunatDocumentCod) {
        String url = UriComponentsBuilder
                .fromHttpUrl(sunatBaseUrl + "/api/v1/sunat/document/signXml")
                .queryParam("SunatDocumentCod", sunatDocumentCod)
                .toUriString();
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, null, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "signXml");
    }

    public void generateZip(String sunatDocumentCod) {
        String url = UriComponentsBuilder
                .fromHttpUrl(sunatBaseUrl + "/api/v1/sunat/document/generateZip")
                .queryParam("SunatDocumentCod", sunatDocumentCod)
                .toUriString();
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, null, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "generateZip");
    }

    public void send(String sunatDocumentCod) {
        String url = UriComponentsBuilder
                .fromHttpUrl(sunatBaseUrl + "/api/v1/sunat/document/send")
                .queryParam("SunatDocumentCod", sunatDocumentCod)
                .toUriString();
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, null, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "send");
    }

    private void validateResponse(SunatWsResponseDto response, String operation) {
        if (response == null) {
            throw new IllegalArgumentException("SUNAT sin respuesta en " + operation);
        }
        if (response.ErrorStatus || !"200".equals(response.Status)) {
            throw new IllegalArgumentException("SUNAT error en " + operation + ": " + response.Message);
        }
    }

    private String extractSunatDocumentCod(SunatWsResponseDto response) {
        JsonNode data = objectMapper.valueToTree(response.Data);
        JsonNode code = data.get("SunatDocumentCod");
        if (code == null || code.asText().isBlank()) {
            throw new IllegalArgumentException("SUNAT no devolvio SunatDocumentCod");
        }
        return code.asText();
    }
}
