package com.ccadmin.app.identity.service;

import com.ccadmin.app.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class SunatIdentitySearchService {

    static final String CONFIG_GROUP = "SunatIdentityServiceUrl";
    static final String FIND_COMPANY_BY_RUC_CONFIG =
            "SunatIdentityFindCompanyByRucUrl";
    static final String FIND_PERSON_BY_DOCUMENT_CONFIG =
            "SunatIdentityFindPersonByDocumentUrl";

    private final BusinessConfigSearchService businessConfigSearchService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public SunatIdentitySearchService(
            BusinessConfigSearchService businessConfigSearchService,
            ObjectMapper objectMapper
    ) {
        this(
                businessConfigSearchService,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    SunatIdentitySearchService(
            BusinessConfigSearchService businessConfigSearchService,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.businessConfigSearchService = businessConfigSearchService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public CompanyIdentityResponseDto findCompanyByRuc(String ruc) {
        String normalizedRuc = ruc == null ? "" : ruc.trim();
        if (!normalizedRuc.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "El RUC debe contener exactamente 11 digitos"
            );
        }

        return executeGet(
                FIND_COMPANY_BY_RUC_CONFIG,
                List.of(new QueryParameter("Ruc", normalizedRuc)),
                CompanyIdentityResponseDto.class
        );
    }

    public PersonIdentityResponseDto findPersonByDocument(
            String documentType,
            String documentNumber
    ) {
        String normalizedDocumentType = clean(documentType);
        String normalizedDocumentNumber = clean(documentNumber);
        if (normalizedDocumentType.isEmpty()) {
            throw new IllegalArgumentException("El tipo de documento es requerido");
        }
        if (normalizedDocumentNumber.isEmpty()) {
            throw new IllegalArgumentException("El numero de documento es requerido");
        }

        return executeGet(
                FIND_PERSON_BY_DOCUMENT_CONFIG,
                List.of(
                        new QueryParameter("DocumentType", normalizedDocumentType),
                        new QueryParameter("DocumentNumber", normalizedDocumentNumber)
                ),
                PersonIdentityResponseDto.class
        );
    }

    private <T> T executeGet(
            String configurationCode,
            List<QueryParameter> queryParameters,
            Class<T> responseType
    ) {
        URI endpoint = buildEndpoint(
                findActiveConfiguration(configurationCode),
                queryParameters
        );
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            validateHttpResponse(response);
            return parseResponse(response.body(), responseType);
        } catch (HttpTimeoutException exception) {
            throw new IllegalStateException(
                    "El servicio de identidad SUNAT excedio el tiempo de espera"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "La consulta de identidad SUNAT fue interrumpida"
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "El servicio de identidad SUNAT no esta disponible"
            );
        }
    }

    private BusinessConfigEntity findActiveConfiguration(String configurationCode) {
        BusinessConfigEntity configuration = businessConfigSearchService
                .findByConfigCod(CONFIG_GROUP, configurationCode);
        if (configuration == null
                || !StatusConst.ACTIVE.equals(configuration.Status)
                || clean(configuration.ConfigVal).isEmpty()) {
            throw new IllegalStateException(
                    "La URL de identidad SUNAT no esta configurada o esta inactiva: "
                            + configurationCode
            );
        }
        return configuration;
    }

    private URI buildEndpoint(
            BusinessConfigEntity configuration,
            List<QueryParameter> queryParameters
    ) {
        String configuredUrl = clean(configuration.ConfigVal);
        StringBuilder endpoint = new StringBuilder(configuredUrl);
        endpoint.append(configuredUrl.contains("?") ? '&' : '?');

        for (int index = 0; index < queryParameters.size(); index++) {
            QueryParameter parameter = queryParameters.get(index);
            if (index > 0) {
                endpoint.append('&');
            }
            endpoint.append(encode(parameter.name()))
                    .append('=')
                    .append(encode(parameter.value()));
        }

        try {
            URI uri = URI.create(endpoint.toString());
            if (!uri.isAbsolute()
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("unsupported URI");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "La URL de identidad SUNAT no es valida: "
                            + configuration.ConfigCod
            );
        }
    }

    private void validateHttpResponse(HttpResponse<String> response) {
        if (response.statusCode() == 408 || response.statusCode() == 504) {
            throw new IllegalStateException(
                    "El servicio de identidad SUNAT excedio el tiempo de espera"
            );
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "El servicio de identidad SUNAT no pudo completar la consulta"
            );
        }
    }

    private <T> T parseResponse(String responseBody, Class<T> responseType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("ErrorStatus").asBoolean(false)) {
                throw new IllegalStateException(
                        "El servicio de identidad SUNAT rechazo la consulta"
                );
            }
            JsonNode data = root.get("Data");
            if (data == null || data.isNull()) {
                throw new IllegalStateException(
                        "El servicio de identidad SUNAT devolvio una respuesta sin datos"
                );
            }
            return objectMapper.treeToValue(data, responseType);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "El servicio de identidad SUNAT devolvio una respuesta invalida"
            );
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record QueryParameter(String name, String value) {
    }
}
