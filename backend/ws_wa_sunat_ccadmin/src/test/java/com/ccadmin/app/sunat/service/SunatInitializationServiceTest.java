package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatConfigDefaults;
import com.ccadmin.app.sunat.model.dto.SunatInitializationFormDto;
import com.ccadmin.app.sunat.model.dto.SunatInitializationRequestDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.repository.SunatConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SunatInitializationServiceTest {
    private SunatConfigRepository sunatConfigRepository;
    private SunatCertificateService sunatCertificateService;
    private SunatConfigCreateService sunatConfigCreateService;
    private SunatInitializationRequestDto request;

    @BeforeEach
    void setUp() {
        sunatConfigRepository = mock(SunatConfigRepository.class);
        sunatCertificateService = mock(SunatCertificateService.class);
        SunatConfigSearchService sunatConfigSearchService = mock(SunatConfigSearchService.class);
        when(sunatConfigSearchService.findInitializationForm()).thenReturn(new SunatInitializationFormDto());
        when(sunatConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        sunatConfigCreateService = new SunatConfigCreateService();
        ReflectionTestUtils.setField(sunatConfigCreateService, "sunatConfigRepository", sunatConfigRepository);
        ReflectionTestUtils.setField(sunatConfigCreateService, "sunatCertificateService", sunatCertificateService);
        ReflectionTestUtils.setField(sunatConfigCreateService, "sunatConfigSearchService", sunatConfigSearchService);
        request = new SunatInitializationRequestDto();
        request.IssuerRuc = "20123456789";
        request.AuditUserCod = "ROOT";
    }

    @Test
    void noneDeactivatesAllWithoutCreatingOrLoadingCertificates() {
        request.Mode = "NONE";
        sunatConfigCreateService.configureInitialization(request, null);
        verify(sunatConfigRepository).deactivateActiveConfigs("ROOT");
        verify(sunatConfigRepository, never()).save(any());
        verifyNoInteractions(sunatCertificateService);
    }

    @Test
    void betaUsesOnlyPublicTestCredentialsAndPortableCertificate() {
        request.Mode = "BETA";
        request.SolPassword = "must-not-be-used";
        request.GuideEnabled = true;
        request.GuideClientSecret = "must-not-be-used";
        sunatConfigCreateService.configureInitialization(request, null);
        SunatConfigEntity saved = saved();
        assertEquals("MODDATOS", saved.SolPassword);
        assertEquals(SunatConfigDefaults.TEST_CERTIFICATE, saved.CertificatePath);
        assertEquals(SunatConfigDefaults.BETA_ENDPOINT, saved.InvoiceEndpoint);
        assertEquals(request.IssuerRuc, saved.IssuerRuc);
        assertEquals("ROOT", saved.CreationUser);
        assertNull(saved.GuideClientSecret);
        verify(sunatCertificateService).load(saved);
    }

    @Test
    void productionRequiresCertificateAndDoesNotDeactivateCurrentWhenInvalid() {
        production();
        assertThrows(IllegalArgumentException.class, () -> sunatConfigCreateService.configureInitialization(request, null));
        verify(sunatConfigRepository, never()).deactivateActiveConfigs(anyString());
        verify(sunatConfigRepository, never()).save(any());
    }

    @Test
    void productionStoresUploadAndUsesFixedProductionUrls() {
        production();
        var upload = new MockMultipartFile("certificate", "production.pfx", "application/octet-stream", new byte[]{1});
        when(sunatCertificateService.storeProduction(upload, "certificate-password")).thenReturn("storage/sunat/certificates/random.pfx");
        sunatConfigCreateService.configureInitialization(request, upload);
        SunatConfigEntity saved = saved();
        assertEquals(SunatConfigDefaults.PRODUCTION_ENDPOINT, saved.InvoiceEndpoint);
        assertEquals("storage/sunat/certificates/random.pfx", saved.CertificatePath);
        assertEquals("sol-password", saved.SolPassword);
        assertEquals("S", saved.ActiveConfig);
    }

    @Test
    void retainsExistingSecretsOnlyForSameIssuer() {
        production();
        SunatConfigEntity existing = SunatConfigDefaults.create("PRODUCCION");
        existing.IssuerRuc = request.IssuerRuc;
        existing.SolPassword = "saved-sol";
        existing.CertificatePath = "storage/sunat/certificates/saved.pfx";
        existing.CertificatePassword = "saved-certificate";
        existing.CreationUser = "SISTEMA";
        existing.CreationDate = new java.util.Date(1000);
        when(sunatConfigRepository.findById(SunatConfigDefaults.PRODUCTION_CODE)).thenReturn(Optional.of(existing));
        request.SolPassword = request.CertificatePassword = "";
        sunatConfigCreateService.configureInitialization(request, null);
        assertEquals("saved-sol", saved().SolPassword);
        assertEquals("SISTEMA", saved().CreationUser);
        assertEquals(new java.util.Date(1000), saved().CreationDate);
        assertEquals("ROOT", saved().ModifyUser);
        request.IssuerRuc = "20999999999";
        assertThrows(IllegalArgumentException.class, () -> sunatConfigCreateService.configureInitialization(request, null));
    }

    @Test
    void missingGuideCredentialsFailBeforeSaving() {
        production();
        request.GuideEnabled = true;
        assertThrows(IllegalArgumentException.class, () -> sunatConfigCreateService.configureInitialization(request, null));
        verify(sunatConfigRepository, never()).save(any());
    }

    @Test
    void rejectedSaveRemovesNewCertificate() {
        production();
        var upload = new MockMultipartFile("certificate", "production.pfx", "application/octet-stream", new byte[]{1});
        when(sunatCertificateService.storeProduction(any(), anyString())).thenReturn("storage/sunat/certificates/new.pfx");
        when(sunatConfigRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> sunatConfigCreateService.configureInitialization(request, upload));
        verify(sunatCertificateService).delete("storage/sunat/certificates/new.pfx");
    }

    @Test
    void formNeverReturnsSavedPasswords() {
        SunatConfigEntity config = SunatConfigDefaults.create("PRODUCCION");
        config.SolUser = "SOLUSER";
        config.SolPassword = "sol-secret";
        config.CertificatePassword = "cert-secret";
        config.CertificatePath = "storage/sunat/certificates/file.pfx";
        when(sunatConfigRepository.findActiveConfig()).thenReturn(Optional.of(config));
        when(sunatConfigRepository.findById(config.SunatConfigCod)).thenReturn(Optional.of(config));
        SunatConfigSearchService search = new SunatConfigSearchService();
        ReflectionTestUtils.setField(search, "sunatConfigRepository", sunatConfigRepository);
        SunatInitializationFormDto form = search.findInitializationForm();
        assertEquals("PRODUCCION", form.Mode);
        assertTrue(form.HasCertificate);
        assertTrue(form.HasCertificatePassword);
        assertTrue(form.HasSolPassword);
        String json = assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(form));
        assertFalse(json.contains("sol-secret"));
        assertFalse(json.contains("cert-secret"));
    }

    private void production() {
        request.Mode = "PRODUCCION";
        request.SolUser = "SOLUSER";
        request.SolPassword = "sol-password";
        request.CertificatePassword = "certificate-password";
    }

    private SunatConfigEntity saved() {
        ArgumentCaptor<SunatConfigEntity> captor = ArgumentCaptor.forClass(SunatConfigEntity.class);
        verify(sunatConfigRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
