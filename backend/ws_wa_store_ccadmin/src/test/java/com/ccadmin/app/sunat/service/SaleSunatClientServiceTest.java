package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.dto.UrlDataDto;
import com.ccadmin.app.shared.shared.UrlSearchShared;
import com.ccadmin.app.sunat.model.constants.SunatSubmissionConstants;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptStartDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatWsResponseDto;
import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleSunatClientServiceTest {

    @Mock
    private UrlSearchShared urlSearchShared;

    @Mock
    private SunatSubmissionAttemptCreateService sunatSubmissionAttemptCreateService;

    @Mock
    private SunatSubmissionSearchService sunatSubmissionSearchService;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private SaleSunatClientService service;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        service = new SaleSunatClientService(
                urlSearchShared,
                sunatSubmissionAttemptCreateService,
                sunatSubmissionSearchService,
                new ObjectMapper(),
                restTemplateBuilder
        );
    }

    @Test
    void inactiveUrlIsStoredAsFailureWithoutCallingTransport() {
        SunatInvoiceProcessRequestDto request = invoiceRequest();
        SunatSubmissionEntity submission = submission();
        when(sunatSubmissionAttemptCreateService.generateCode())
                .thenReturn(submission.SunatSubmissionCod);
        when(sunatSubmissionAttemptCreateService.beginInitialAttempt(
                anyString(), anyString(), anyString(), anyString(), same(request), anyString()
        )).thenReturn(new SunatSubmissionAttemptStartDto(submission, true));
        UrlDataDto urlData = mock(UrlDataDto.class);
        urlData.status = "I";
        when(urlSearchShared.findUrlDtaSunat("01_invoice")).thenReturn(urlData);

        SunatWsResponseDto response = service.processInvoice(request);

        assertTrue(response.ErrorStatus);
        ArgumentCaptor<SunatSubmissionAttemptResultDto> resultCaptor =
                ArgumentCaptor.forClass(SunatSubmissionAttemptResultDto.class);
        verify(sunatSubmissionAttemptCreateService).finishAttempt(
                eq(submission.SunatSubmissionCod),
                eq("USER01"),
                resultCaptor.capture()
        );
        assertFalse(resultCaptor.getValue().Successful);
        assertTrue(resultCaptor.getValue().ErrorReason.contains("inactiva"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void successfulResponseStoresRemoteSunatState() {
        SunatInvoiceProcessRequestDto request = invoiceRequest();
        SunatSubmissionEntity submission = submission();
        when(sunatSubmissionAttemptCreateService.generateCode())
                .thenReturn(submission.SunatSubmissionCod);
        when(sunatSubmissionAttemptCreateService.beginInitialAttempt(
                anyString(), anyString(), anyString(), anyString(), same(request), anyString()
        )).thenReturn(new SunatSubmissionAttemptStartDto(submission, true));
        UrlDataDto urlData = mock(UrlDataDto.class);
        urlData.status = "A";
        urlData.urlAddress = "http://sunat.test/process";
        when(urlSearchShared.findUrlDtaSunat("01_invoice")).thenReturn(urlData);

        SunatWsResponseDto response = new SunatWsResponseDto();
        response.Status = "200";
        response.Message = "OK";
        response.Data = new LinkedHashMap<>(Map.of(
                "Processed", true,
                "ElectronicStatus", "ACE",
                "SunatDocumentCod", "SD0001"
        ));
        when(restTemplate.postForEntity(
                eq(urlData.urlAddress), same(request), eq(SunatWsResponseDto.class)
        )).thenReturn(ResponseEntity.ok(response));

        SunatWsResponseDto result = service.processInvoice(request);

        assertSame(response, result);
        ArgumentCaptor<SunatSubmissionAttemptResultDto> resultCaptor =
                ArgumentCaptor.forClass(SunatSubmissionAttemptResultDto.class);
        verify(sunatSubmissionAttemptCreateService).finishAttempt(
                eq(submission.SunatSubmissionCod),
                eq("USER01"),
                resultCaptor.capture()
        );
        assertTrue(resultCaptor.getValue().Successful);
        assertEquals("ACE", resultCaptor.getValue().SunatStatus);
        assertEquals("SD0001", resultCaptor.getValue().RemoteSunatDocumentCod);
    }

    @Test
    void localAuditMetadataIsNotAddedToRemotePayload() throws Exception {
        String payload = new ObjectMapper().writeValueAsString(invoiceRequest());

        assertFalse(payload.contains("StoreCod"));
        assertFalse(payload.contains("AuditUserCod"));
        assertTrue(payload.contains("SourceDocumentCod"));
    }

    private SunatInvoiceProcessRequestDto invoiceRequest() {
        SunatInvoiceProcessRequestDto request = new SunatInvoiceProcessRequestDto();
        request.StoreCod = "L001";
        request.AuditUserCod = "USER01";
        request.SourceModule = "SALE";
        request.SourceDocumentCod = "VE000001";
        request.SourceDocumentType = "SALE";
        request.Series = "F001";
        request.Correlative = 1;
        return request;
    }

    private SunatSubmissionEntity submission() {
        SunatSubmissionEntity submission = new SunatSubmissionEntity();
        submission.SunatSubmissionCod = "ES000000000000000001";
        submission.RequestType = SunatSubmissionConstants.REQUEST_TYPE_INVOICE;
        submission.EndpointKey = "01_invoice";
        return submission;
    }
}
