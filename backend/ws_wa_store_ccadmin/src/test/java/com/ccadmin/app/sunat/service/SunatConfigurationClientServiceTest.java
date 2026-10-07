package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SunatConfigurationClientServiceTest {
    private static final String ENDPOINT = "http://localhost:8092/api/v1/sunat/config/initialization";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private SunatConfigurationClientService sunatConfigurationClientService;

    @BeforeEach
    void setUp() {
        BusinessConfigSearchService businessConfigSearchService = mock(BusinessConfigSearchService.class);
        var config = new BusinessConfigEntity();
        config.ConfigDesc = "http://localhost:8092/api/v1/sunat/invoice/process";
        config.Status = "I";
        when(businessConfigSearchService.findByConfigCod("UrlServiciosSunat", "01_invoice")).thenReturn(config);
        RestTemplateBuilder builder = new RestTemplateBuilder().additionalCustomizers(template ->
                server = MockRestServiceServer.bindTo(template).build());
        sunatConfigurationClientService = new SunatConfigurationClientService(businessConfigSearchService, objectMapper, builder);
    }

    @Test
    void readsConfigurationEvenWhenDocumentSendingUrlIsInactive() {
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ErrorStatus\":false,\"Data\":{\"Mode\":\"NONE\"}}", MediaType.APPLICATION_JSON));
        assertEquals("NONE", sunatConfigurationClientService.findInitializationForm().path("Mode").asText());
        server.verify();
    }

    @Test
    void forwardsConfigurationAndCertificateAsMultipartWithoutOriginalFilename() {
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("name=\"configuration\"")))
                .andExpect(content().string(containsString("filename=\"certificate.pfx\"")))
                .andExpect(content().string(containsString("\"Mode\":\"PRODUCCION\"")))
                .andRespond(withSuccess("{\"ErrorStatus\":false,\"Data\":{\"Mode\":\"PRODUCCION\"}}", MediaType.APPLICATION_JSON));
        var upload = new MockMultipartFile("certificate", "private-company.pfx", "application/octet-stream", new byte[]{1, 2});
        var result = sunatConfigurationClientService.configure(objectMapper.createObjectNode().put("Mode", "PRODUCCION"), upload);
        assertEquals("PRODUCCION", result.path("Mode").asText());
        server.verify();
    }

    @Test
    void rejectsRemoteFunctionalError() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"ErrorStatus\":true,\"Message\":\"Certificado invalido\"}"));
        assertEquals("Certificado invalido", assertThrows(IllegalArgumentException.class,
                () -> sunatConfigurationClientService.findInitializationForm()).getMessage());
        server.verify();
    }
}
