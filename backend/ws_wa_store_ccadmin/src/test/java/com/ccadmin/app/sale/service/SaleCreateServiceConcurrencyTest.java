package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleCreateServiceConcurrencyTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @InjectMocks
    private SaleCreateService saleCreateService;

    @Test
    void shouldLockSaleAndRejectConfirmationRetry() {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "SL001";
        sale.SaleStatus = SaleConstants.CONFIRMED;
        when(this.saleHeadRepository.findByIdForUpdate("SL001")).thenReturn(Optional.of(sale));
        when(this.saleDetWarehouseRepository.findBySaleCod("SL001"))
                .thenReturn(List.of(new SaleDetWarehouseEntity()));

        assertThatThrownBy(() -> this.saleCreateService.confirm("SL001", "01", "CF001"))
                .isInstanceOf(SaleException.class)
                .hasMessageContaining("ya no se encuentra pendiente");

        verify(this.saleHeadRepository).findByIdForUpdate("SL001");
    }
}
