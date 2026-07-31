package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private KardexShared kardexShared;
    @Mock
    private KardexZoneRepository kardexZoneRepository;
    @Mock
    private SaleSearchService saleSearchService;
    @Mock
    private CreditNoteApplicationCreateService creditNoteApplicationCreateService;
    @InjectMocks
    private ExpiredSaleCancellationService cancellationService;

    @Test
    void forceCancellationRejectsPresaleWithReservation() {
        PresaleHeadEntity presale = confirmedPresale();
        when(presaleHeadRepository.findByIdForUpdate(presale.PresaleCod))
                .thenReturn(Optional.of(presale));
        when(kardexZoneRepository.countByOperationEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                presale.PresaleCod,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        )).thenReturn(2);

        SaleException exception = assertThrows(
                SaleException.class,
                () -> cancellationService.cancelPresale(presale.PresaleCod, true)
        );

        assertEquals(
                "La preventa tiene stock reservado. Debe realizar la anulacion regular",
                exception.getMessage()
        );
        verify(saleHeadRepository, never()).findByPresaleCodForUpdate(presale.PresaleCod);
    }

    @Test
    void regularCancellationRejectsConfirmedLegacyPresaleWithoutReservation() {
        PresaleHeadEntity presale = confirmedPresale();
        when(presaleHeadRepository.findByIdForUpdate(presale.PresaleCod))
                .thenReturn(Optional.of(presale));
        when(kardexZoneRepository.countByOperationEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                presale.PresaleCod,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        )).thenReturn(0);

        SaleException exception = assertThrows(
                SaleException.class,
                () -> cancellationService.cancelPresale(presale.PresaleCod, false)
        );

        assertEquals(
                "La preventa no tiene stock reservado. Use la anulacion forzada",
                exception.getMessage()
        );
    }

    @Test
    void forceCancellationChangesStatusesWhenLegacyPresaleHasNoReservationOrPayments() throws Exception {
        PresaleHeadEntity presale = confirmedPresale();
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        sale.PresaleCod = presale.PresaleCod;
        sale.SaleStatus = SaleConstants.PENDING;

        when(presaleHeadRepository.findByIdForUpdate(presale.PresaleCod))
                .thenReturn(Optional.of(presale));
        when(kardexZoneRepository.countByOperationEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                presale.PresaleCod,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        )).thenReturn(0);
        when(saleHeadRepository.findByPresaleCodForUpdate(presale.PresaleCod))
                .thenReturn(Optional.of(sale));
        when(salePaymentRepository.findTotalPaymentExcludingCreditNoteApplications(sale.SaleCod))
                .thenReturn(BigDecimal.ZERO);
        when(presaleHeadRepository.findById(presale.PresaleCod)).thenReturn(Optional.of(presale));
        when(saleHeadRepository.findByPresaleCod(presale.PresaleCod)).thenReturn(Optional.of(sale));
        when(saleSearchService.findById(sale.SaleCod)).thenReturn(new SaleDetailDto());

        cancellationService.cancelPresale(presale.PresaleCod, true);

        assertEquals(StatusConst.CANCELLED, presale.SaleStatus);
        assertEquals(SaleConstants.CANCELLED, sale.SaleStatus);
        verify(kardexShared, never()).saveAll(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }

    private PresaleHeadEntity confirmedPresale() {
        PresaleHeadEntity presale = new PresaleHeadEntity();
        presale.PresaleCod = "PS001";
        presale.SaleStatus = StatusConst.CONFIRMED;
        return presale;
    }
}
