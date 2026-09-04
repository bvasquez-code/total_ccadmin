package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatSubmissionConstants;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptStartDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.ccadmin.app.sunat.repository.SunatSubmissionRepository;
import com.ccadmin.app.system.shared.TableSequenceShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SunatSubmissionAttemptCreateServiceTest {

    @Mock
    private SunatSubmissionRepository sunatSubmissionRepository;

    @Mock
    private TableSequenceShared tableSequenceShared;

    private SunatSubmissionAttemptCreateService service;

    @BeforeEach
    void setUp() {
        service = new SunatSubmissionAttemptCreateService(
                sunatSubmissionRepository,
                tableSequenceShared
        );
        lenient().when(sunatSubmissionRepository.save(any(SunatSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void beginInitialAttemptPersistsSendingStateBeforeTransport() {
        SunatInvoiceProcessRequestDto request = invoiceRequest();
        when(sunatSubmissionRepository.findBySourceForUpdate("SALE", "VE000001", "01"))
                .thenReturn(Optional.empty());

        SunatSubmissionAttemptStartDto result = service.beginInitialAttempt(
                "ES000000000000000001",
                SunatSubmissionConstants.REQUEST_TYPE_INVOICE,
                "01_invoice",
                "01",
                request,
                "{\"SourceModule\":\"SALE\"}"
        );

        assertTrue(result.sendRequired());
        assertEquals(SunatSubmissionConstants.SEND_STATUS_SENDING,
                result.submission().SendStatus);
        assertEquals(1, result.submission().AttemptCount);
        assertEquals("L001", result.submission().StoreCod);
        assertEquals("USER01", result.submission().CreationUser);
        assertNotNull(result.submission().LastAttemptDate);
        verify(sunatSubmissionRepository).save(result.submission());
    }

    @Test
    void beginInitialAttemptDoesNotDuplicateASentDocument() {
        SunatInvoiceProcessRequestDto request = invoiceRequest();
        SunatSubmissionEntity existing = new SunatSubmissionEntity();
        existing.SunatSubmissionCod = "ES000000000000000001";
        existing.SendStatus = SunatSubmissionConstants.SEND_STATUS_SENT;
        existing.AttemptCount = 1;
        when(sunatSubmissionRepository.findBySourceForUpdate("SALE", "VE000001", "01"))
                .thenReturn(Optional.of(existing));

        SunatSubmissionAttemptStartDto result = service.beginInitialAttempt(
                "ES000000000000000002",
                SunatSubmissionConstants.REQUEST_TYPE_INVOICE,
                "01_invoice",
                "01",
                request,
                "{\"SourceModule\":\"SALE\"}"
        );

        assertFalse(result.sendRequired());
        assertSame(existing, result.submission());
        assertEquals(1, existing.AttemptCount);
        verify(sunatSubmissionRepository, never()).save(any());
    }

    @Test
    void finishAttemptStoresFailureReasonAndRemoteState() {
        SunatSubmissionEntity existing = new SunatSubmissionEntity();
        existing.SunatSubmissionCod = "ES000000000000000001";
        existing.SendStatus = SunatSubmissionConstants.SEND_STATUS_SENDING;
        existing.AttemptCount = 1;
        when(sunatSubmissionRepository.findForUpdate(existing.SunatSubmissionCod))
                .thenReturn(existing);
        SunatSubmissionAttemptResultDto result = new SunatSubmissionAttemptResultDto();
        result.Successful = false;
        result.ResponseStatus = "200";
        result.ResponseJson = "{\"Status\":\"200\"}";
        result.SunatStatus = "REJ";
        result.RemoteSunatDocumentCod = "SD0001";
        result.ErrorReason = "Comprobante rechazado por SUNAT";

        SunatSubmissionEntity updated = service.finishAttempt(
                existing.SunatSubmissionCod,
                "USER02",
                result
        );

        assertEquals(SunatSubmissionConstants.SEND_STATUS_ERROR, updated.SendStatus);
        assertEquals("REJ", updated.SunatStatus);
        assertEquals("SD0001", updated.RemoteSunatDocumentCod);
        assertEquals("Comprobante rechazado por SUNAT", updated.LastErrorReason);
        assertEquals("USER02", updated.ModifyUser);
        assertNull(updated.LastSuccessDate);
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
}
