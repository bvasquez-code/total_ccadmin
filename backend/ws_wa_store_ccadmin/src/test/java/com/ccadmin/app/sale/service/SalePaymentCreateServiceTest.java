package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePaymentCreateServiceTest {

    @Mock
    private SalePaymentRepository salePaymentRepository;
    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private TrxPaymentShared trxPaymentShared;
    @Mock
    private SaleCreateService saleCreateService;
    @InjectMocks
    private SalePaymentCreateService salePaymentCreateService;

    @Test
    void shouldRejectPaymentWhenSaleWasCancelled() {
        SalePaymentRegisterDto payment = new SalePaymentRegisterDto();
        payment.SaleCod = "SL001";
        payment.TrxPaymentId = 1L;
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "SL001";
        sale.SaleStatus = SaleConstants.CANCELLED;
        when(this.saleHeadRepository.findByIdForUpdate("SL001")).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> this.salePaymentCreateService.save(payment))
                .isInstanceOf(SalePaymentException.class)
                .hasMessageContaining("no longer pending");

        verify(this.trxPaymentShared, never()).findById(1L);
    }
}
