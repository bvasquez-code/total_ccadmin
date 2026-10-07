package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;

@Service
public class SunatConfigurationClientService {
    private final BusinessConfigSearchService businessConfigSearchService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SunatConfigurationClientService(BusinessConfigSearchService businessConfigSearchService,
                                          ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.businessConfigSearchService = businessConfigSearchService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30)).build();
    }

    public ObjectNode findInitializationForm() {
        try { return responseData(restTemplate.getForObject(endpoint(), JsonNode.class)); }
        catch (RestClientResponseException exception) { throw remoteError(exception); }
    }

    public ObjectNode configure(ObjectNode configuration, MultipartFile certificate) {
        try {
            LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("configuration", objectMapper.writeValueAsString(configuration));
            if (certificate != null && !certificate.isEmpty()) {
                if (certificate.getSize() > 2 * 1024 * 1024) {
                    throw new IllegalArgumentException("El certificado no debe superar 2 MB");
                }
                byte[] content = certificate.getBytes();
                String originalFileName = certificate.getOriginalFilename();
                if (originalFileName == null || !originalFileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pfx")) {
                    throw new IllegalArgumentException("Seleccione un certificado .pfx");
                }
                parts.add("certificate", new ByteArrayResource(content) {
                    @Override public String getFilename() { return "certificate.pfx"; }
                });
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            return responseData(restTemplate.postForObject(endpoint(), new HttpEntity<>(parts, headers), JsonNode.class));
        } catch (RestClientResponseException exception) { throw remoteError(exception); }
        catch (java.io.IOException exception) { throw new IllegalArgumentException("No se pudo preparar el certificado", exception); }
    }

    private String endpoint() {
        BusinessConfigEntity invoiceUrl = businessConfigSearchService.findByConfigCod("UrlServiciosSunat", "01_invoice");
        if (invoiceUrl == null || invoiceUrl.ConfigDesc == null || invoiceUrl.ConfigDesc.isBlank()) {
            throw new IllegalArgumentException("Configure la dirección del servicio SUNAT en UrlServiciosSunat / 01_invoice");
        }
        URI invoice = URI.create(invoiceUrl.ConfigDesc);
        String path = invoice.getPath();
        int apiIndex = path == null ? -1 : path.indexOf("/api/v1/");
        if (apiIndex < 0 || invoice.getHost() == null
                || !("http".equals(invoice.getScheme()) || "https".equals(invoice.getScheme()))) {
            throw new IllegalArgumentException("La dirección del servicio SUNAT no es válida");
        }
        return invoice.getScheme() + "://" + invoice.getRawAuthority() + path.substring(0, apiIndex)
                + "/api/v1/sunat/config/initialization";
    }

    private ObjectNode responseData(JsonNode response) {
        if (response == null || response.path("ErrorStatus").asBoolean(true) || !response.path("Data").isObject()) {
            throw new IllegalArgumentException(response == null ? "El servicio SUNAT no respondió"
                    : response.path("Message").asText("No se pudo configurar SUNAT"));
        }
        return (ObjectNode) response.path("Data");
    }

    private IllegalArgumentException remoteError(RestClientResponseException exception) {
        String message = "No se pudo configurar el servicio SUNAT";
        try { message = objectMapper.readTree(exception.getResponseBodyAsString()).path("Message").asText(message); }
        catch (Exception ignored) { /* No devolver cuerpos HTTP ni credenciales al navegador. */ }
        return new IllegalArgumentException(message);
    }
}
