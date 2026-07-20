package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.exception.PresaleException;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresaleCreateServiceConcurrencyTest {

    @Mock
    private PresaleHeadRepository presaleHeadRepository;
    @Mock
    private PresaleSearchService presaleSearchService;
    @Mock
    private SaleCreateService saleCreateService;
    @Mock
    private PresaleStockReservationService presaleStockReservationService;
    @InjectMocks
    private PresaleCreateService presaleCreateService;

    @Test
    void shouldLockPresaleBeforeCreatingPendingSaleAndReservation() throws Exception {
        PresaleRegisterDto request = request();
        PresaleHeadEntity presale = pendingPresale();
        PresaleDetailDto presaleDetail = new PresaleDetailDto();
        presaleDetail.Headboard = presale;
        SaleDetailDto saleDetail = new SaleDetailDto();
        saleDetail.Headboard = pendingSale();
        when(this.presaleHeadRepository.findByIdForUpdate("PS001"))
                .thenReturn(Optional.of(presale));
        when(this.presaleSearchService.findById("PS001")).thenReturn(presaleDetail);
        when(this.saleCreateService.save(presaleDetail)).thenReturn(saleDetail);

        SaleDetailDto result = this.presaleCreateService.confirm(request);

        assertThat(result).isSameAs(saleDetail);
        assertThat(presale.SaleStatus).isEqualTo(StatusConst.CONFIRMED);
        verify(this.presaleHeadRepository).findByIdForUpdate("PS001");
        verify(this.presaleStockReservationService).reserve(presale, saleDetail.Headboard, "SISTEMA");
    }

    @Test
    void shouldRejectConfirmationRetryAfterPresaleWasConfirmed() throws Exception {
        PresaleRegisterDto request = request();
        PresaleHeadEntity presale = pendingPresale();
        presale.SaleStatus = StatusConst.CONFIRMED;
        when(this.presaleHeadRepository.findByIdForUpdate("PS001"))
                .thenReturn(Optional.of(presale));

        assertThatThrownBy(() -> this.presaleCreateService.confirm(request))
                .isInstanceOf(PresaleException.class)
                .hasMessageContaining("already been confirmed");

        verify(this.saleCreateService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(this.presaleStockReservationService, never()).reserve(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private PresaleRegisterDto request() {
        PresaleRegisterDto request = new PresaleRegisterDto();
        request.Headboard = new PresaleHeadEntity();
        request.Headboard.PresaleCod = "PS001";
        return request;
    }

    private PresaleHeadEntity pendingPresale() {
        PresaleHeadEntity presale = new PresaleHeadEntity();
        presale.PresaleCod = "PS001";
        presale.StoreCod = "S001";
        presale.SaleStatus = StatusConst.PENDING;
        return presale;
    }

    private SaleHeadEntity pendingSale() {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "SL001";
        sale.PresaleCod = "PS001";
        sale.StoreCod = "S001";
        sale.SaleStatus = StatusConst.PENDING;
        return sale;
    }
}
