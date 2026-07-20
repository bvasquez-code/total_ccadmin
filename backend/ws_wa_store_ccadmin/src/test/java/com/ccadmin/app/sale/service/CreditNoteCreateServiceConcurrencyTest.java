package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteRegisterDto;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.repository.CreditNoteHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteCreateServiceConcurrencyTest {

    @Mock
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @InjectMocks
    private CreditNoteCreateService creditNoteCreateService;

    @Test
    void shouldLockAndRejectConfirmationRetry() {
        CreditNoteHeadEntity head = new CreditNoteHeadEntity();
        head.CreditNoteCod = "NC001";
        head.CreditNoteStatus = SaleConstants.CONFIRMED;
        CreditNoteRegisterDto request = new CreditNoteRegisterDto();
        request.Headboard = new CreditNoteHeadEntity();
        request.Headboard.CreditNoteCod = "NC001";
        when(this.creditNoteHeadRepository.findByIdForUpdate("NC001")).thenReturn(Optional.of(head));

        assertThatThrownBy(() -> this.creditNoteCreateService.confirm(request))
                .isInstanceOf(SaleException.class)
                .hasMessageContaining("ya fue confirmada");

        verify(this.creditNoteHeadRepository).findByIdForUpdate("NC001");
    }

    @Test
    void shouldLockAndRejectStockReturnRetry() {
        CreditNoteHeadEntity head = new CreditNoteHeadEntity();
        head.CreditNoteCod = "NC001";
        head.CreditNoteStatus = SaleConstants.CONFIRMED;
        head.IsStockReturned = "S";
        CreditNoteRegisterDto request = new CreditNoteRegisterDto();
        request.Headboard = new CreditNoteHeadEntity();
        request.Headboard.CreditNoteCod = "NC001";
        when(this.creditNoteHeadRepository.findByIdForUpdate("NC001")).thenReturn(Optional.of(head));

        assertThatThrownBy(() -> this.creditNoteCreateService.saveReturnStock(request))
                .isInstanceOf(SaleException.class)
                .hasMessageContaining("ya fue procesado");

        verify(this.creditNoteHeadRepository).findByIdForUpdate("NC001");
    }
}
