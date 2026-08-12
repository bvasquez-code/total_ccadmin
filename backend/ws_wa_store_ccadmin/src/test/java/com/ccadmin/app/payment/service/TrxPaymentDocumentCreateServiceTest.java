package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrxPaymentDocumentCreateServiceTest {

    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Mock private TrxPaymentDocumentRepository trxPaymentDocumentRepository;
    @InjectMocks private TrxPaymentDocumentCreateService service;

    @Test
    void savesWebProofWithCalculatedMetadataAndWebAudit() {
        TrxPaymentDocumentEntity document = paymentProof();
        when(trxPaymentDocumentRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.validateWebPaymentProofs(List.of(document), true);
        service.saveWeb(25L, List.of(document));

        ArgumentCaptor<List<TrxPaymentDocumentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(trxPaymentDocumentRepository).saveAll(captor.capture());
        TrxPaymentDocumentEntity savedDocument = captor.getValue().get(0);
        assertEquals(25L, savedDocument.TrxPaymentId);
        assertEquals("WEB", savedDocument.SourceType);
        assertEquals("USER_WEB", savedDocument.CreationUser);
        assertEquals("A", savedDocument.Status);
        assertNotNull(savedDocument.SizeBytes);
        assertEquals(64, savedDocument.Sha256Hash.length());
    }

    @Test
    void rejectsMissingRequiredWebProof() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateWebPaymentProofs(List.of(), true)
        );
    }

    @Test
    void rejectsTextAsWebPaymentProof() {
        TrxPaymentDocumentEntity document = paymentProof();
        document.ContentEncoding = "TEXT";
        document.Content = "voucher";

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateWebPaymentProofs(List.of(document), true)
        );
    }

    private TrxPaymentDocumentEntity paymentProof() {
        TrxPaymentDocumentEntity document = new TrxPaymentDocumentEntity();
        document.DocumentType = "PAYMENT_PROOF";
        document.ContentEncoding = "BASE64";
        document.Content = ONE_PIXEL_PNG;
        document.FileName = "proof.png";
        document.ContentType = "image/png";
        document.SourceType = "WEB";
        return document;
    }
}
