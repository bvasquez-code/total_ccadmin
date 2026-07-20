package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredSaleCancellationServiceTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private PresaleHeadRepository presaleHeadRepository;
    @Mock
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Mock
    private SalePaymentRepository salePaymentRepository;
    @Mock
    private SaleDocumentRepository saleDocumentRepository;
    @Mock
    private SaleStockConfirmationService saleStockConfirmationService;
    @Mock
    private KardexZoneShared kardexZoneShared;
    @InjectMocks
    private ExpiredSaleCancellationService cancellationService;

    @Test
    void shouldCancelExpiredSaleWithoutPaymentsAndReleaseReservation() throws SaleException {
        Date expirationLimit = new Date(2_000_000L);
        SaleHeadEntity sale = pendingSale(new Date(1_000_000L));
        PresaleHeadEntity presale = confirmedPresale();
        SaleDetWarehouseEntity detail = detail();
        when(this.saleHeadRepository.findByIdForUpdate("SL001")).thenReturn(Optional.of(sale));
        when(this.salePaymentRepository.countTotalPayment("SL001")).thenReturn(0);
        when(this.presaleHeadRepository.findByIdForUpdate("PS001")).thenReturn(Optional.of(presale));
        when(this.saleDetWarehouseRepository.findBySaleCod("SL001")).thenReturn(List.of(detail));
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);

        boolean cancelled = this.cancellationService.cancelExpiredSale(
                "SL001",
                expirationLimit,
                "SYSTEM"
        );

        assertThat(cancelled).isTrue();
        assertThat(sale.SaleStatus).isEqualTo(SaleConstants.CANCELLED);
        assertThat(presale.SaleStatus).isEqualTo(StatusConst.CANCELLED);
        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("SYSTEM"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.MovementEvent)
                .isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_EXPIRATION_RELEASE);
        assertThat(operation.MovementList).hasSize(2);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_RESERVED);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(-10);
        assertThat(operation.MovementList.get(1).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(1).NumStockDelta).isEqualTo(10);
    }

    @Test
    void shouldNotCancelExpiredSaleWhenAnyPaymentExists() {
        Date expirationLimit = new Date(2_000_000L);
        SaleHeadEntity sale = pendingSale(new Date(1_000_000L));
        when(this.saleHeadRepository.findByIdForUpdate("SL001")).thenReturn(Optional.of(sale));
        when(this.salePaymentRepository.countTotalPayment("SL001")).thenReturn(1);

        assertThatThrownBy(() -> this.cancellationService.cancelExpiredSale(
                "SL001",
                expirationLimit,
                "SYSTEM"
        )).isInstanceOf(SaleException.class)
                .hasMessageContaining("tiene pagos");

        assertThat(sale.SaleStatus).isEqualTo(SaleConstants.PENDING);
        verify(this.kardexZoneShared, never()).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("SYSTEM")
        );
    }

    @Test
    void shouldIgnoreCancellationRetryAfterSaleWasAlreadyCancelled() throws SaleException {
        SaleHeadEntity sale = pendingSale(new Date(1_000_000L));
        sale.SaleStatus = SaleConstants.CANCELLED;
        when(this.saleHeadRepository.findByIdForUpdate("SL001")).thenReturn(Optional.of(sale));

        boolean cancelled = this.cancellationService.cancelExpiredSale(
                "SL001",
                new Date(2_000_000L),
                "SYSTEM"
        );

        assertThat(cancelled).isFalse();
        verify(this.salePaymentRepository, never()).countTotalPayment("SL001");
        verify(this.kardexZoneShared, never()).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("SYSTEM")
        );
    }

    private SaleHeadEntity pendingSale(Date creationDate) {
        SaleHeadEntity entity = new SaleHeadEntity();
        entity.SaleCod = "SL001";
        entity.PresaleCod = "PS001";
        entity.StoreCod = "S001";
        entity.SaleStatus = SaleConstants.PENDING;
        entity.CreationDate = creationDate;
        return entity;
    }

    private PresaleHeadEntity confirmedPresale() {
        PresaleHeadEntity entity = new PresaleHeadEntity();
        entity.PresaleCod = "PS001";
        entity.StoreCod = "S001";
        entity.SaleStatus = StatusConst.CONFIRMED;
        return entity;
    }

    private SaleDetWarehouseEntity detail() {
        SaleDetWarehouseEntity entity = new SaleDetWarehouseEntity();
        entity.SaleCod = "SL001";
        entity.ItemNumber = 1;
        entity.ProductCod = "P001";
        entity.Variant = "0000";
        entity.WarehouseCod = "W001";
        entity.NumUnit = 10;
        return entity;
    }
}
