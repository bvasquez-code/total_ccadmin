package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigCreateService;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SunatInitializationCreateServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private SunatConfigurationClientService sunatConfigurationClientService;
    private BusinessConfigCreateService businessConfigCreateService;
    private BusinessConfigSearchService businessConfigSearchService;
    private SunatInitializationCreateService sunatInitializationCreateService;
    private List<BusinessConfigEntity> urls;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("ROOT", "unused"));
        SunatInitializationSearchService search = mock(SunatInitializationSearchService.class);
        when(search.issuerRuc()).thenReturn("20123456789");
        sunatConfigurationClientService = mock(SunatConfigurationClientService.class);
        when(sunatConfigurationClientService.configure(any(), any())).thenReturn(objectMapper.createObjectNode());
        businessConfigSearchService = mock(BusinessConfigSearchService.class);
        businessConfigCreateService = mock(BusinessConfigCreateService.class);
        urls = List.of("01_invoice", "03_receipt", "07_creditNote", "08_debitNote", "09_despatchAdvice", "unrelated")
                .stream().map(code -> { var url = new BusinessConfigEntity(); url.ConfigCod = code; return url; }).toList();
        when(businessConfigSearchService.findByGroupCod("UrlServiciosSunat")).thenReturn(urls);
        sunatInitializationCreateService = new SunatInitializationCreateService(search, sunatConfigurationClientService,
                businessConfigSearchService, businessConfigCreateService, objectMapper);
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void noneDisablesOnlySunatDocumentUrls() throws Exception {
        sunatInitializationCreateService.configure("{\"Mode\":\"NONE\"}", null);
        verify(businessConfigCreateService, times(5)).disable(any());
        verify(businessConfigCreateService, never()).disable(urls.getLast());
        verify(businessConfigCreateService, never()).enable(any());
    }

    @Test
    void betaEnablesInvoicesAndNotesButNeverProductionGre() throws Exception {
        sunatInitializationCreateService.configure("{\"Mode\":\"BETA\",\"GuideEnabled\":true}", null);
        verify(businessConfigCreateService, times(4)).enable(any());
        verify(businessConfigCreateService, never()).enable(urls.get(4));
    }

    @Test
    void productionEnablesGreOnlyAfterRemoteConfirmation() throws Exception {
        when(sunatConfigurationClientService.configure(any(), any())).thenReturn(objectMapper.createObjectNode().put("GuideEnabled", true));
        sunatInitializationCreateService.configure("{\"Mode\":\"PRODUCCION\",\"GuideEnabled\":true}", null);
        verify(businessConfigCreateService, times(5)).enable(any());
    }

    @Test
    void serverSuppliesIssuerAndAuditUserInsteadOfTrustingBrowser() throws Exception {
        sunatInitializationCreateService.configure("{\"Mode\":\"BETA\",\"IssuerRuc\":\"20999999999\",\"AuditUserCod\":\"OTHER\"}", null);
        ArgumentCaptor<ObjectNode> request = ArgumentCaptor.forClass(ObjectNode.class);
        verify(sunatConfigurationClientService).configure(request.capture(), isNull());
        assertEquals("ROOT", request.getValue().path("AuditUserCod").asText());
        assertEquals("20123456789", request.getValue().path("IssuerRuc").asText());
    }

    @Test
    void remoteRejectionNeverEnablesSending() {
        when(sunatConfigurationClientService.configure(any(), any())).thenThrow(new IllegalArgumentException("invalid certificate"));
        assertThrows(IllegalArgumentException.class, () -> sunatInitializationCreateService.configure("{\"Mode\":\"PRODUCCION\"}", null));
        verify(businessConfigCreateService, never()).enable(any());
    }

    @Test
    void missingUrlsFailBeforeRemoteActivation() {
        when(businessConfigSearchService.findByGroupCod("UrlServiciosSunat")).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> sunatInitializationCreateService.configure("{\"Mode\":\"BETA\"}", null));
        verifyNoInteractions(sunatConfigurationClientService);
    }

    @Test
    void nonRootCannotUseInitialization() {
        SunatInitializationSearchService search = new SunatInitializationSearchService(sunatConfigurationClientService, null);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("USER", "unused"));
        assertThrows(IllegalArgumentException.class, search::requireRoot);
    }
}
