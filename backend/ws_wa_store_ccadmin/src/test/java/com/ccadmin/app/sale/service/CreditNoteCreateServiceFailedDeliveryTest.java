package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteRegisterDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleChannelEntity;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.repository.CreditNoteHeadRepository;
import com.ccadmin.app.sale.repository.SaleChannelRepository;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteCreateServiceFailedDeliveryTest {

    @Mock
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Mock
    private SaleDetRepository saleDetRepository;
    @Mock
    private SaleChannelRepository saleChannelRepository;
    @Mock
    private SaleDeliveryRepository saleDeliveryRepository;
    @InjectMocks
    private CreditNoteCreateService creditNoteCreateService;

    @Test
    void rejectsPartialCreditNoteForFailedDelivery() {
        CreditNoteRegisterDto request = request();
        SaleDetEntity saleDetail = new SaleDetEntity();
        saleDetail.ItemNumber = 1;
        saleDetail.ProductCod = "P001";
        saleDetail.Variant = "0000";
        saleDetail.NumUnit = 2;

        SaleChannelEntity channel = new SaleChannelEntity();
        channel.SaleCod = "ST001";
        channel.ChannelCod = SaleConstants.COMMERCIAL_CHANNEL_WEB;
        SaleDeliveryEntity delivery = new SaleDeliveryEntity();
        delivery.SaleCod = "ST001";
        delivery.DeliveryStatus = SaleConstants.DELIVERY_STATUS_FAILED;

        when(saleDetRepository.findBySaleCod("ST001")).thenReturn(List.of(saleDetail));
        when(saleChannelRepository.findActiveBySaleCod("ST001")).thenReturn(Optional.of(channel));
        when(saleDeliveryRepository.findActiveBySaleCod("ST001")).thenReturn(Optional.of(delivery));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> creditNoteCreateService.save(request)
        );

        assertEquals(
                "La entrega fallida requiere una nota de credito total con devolucion de pagos",
                exception.getMessage()
        );
    }

    private CreditNoteRegisterDto request() {
        CreditNoteHeadEntity head = new CreditNoteHeadEntity();
        head.CreditNoteCod = "NC001";
        head.SaleCod = "ST001";
        head.CreditNoteStatus = SaleConstants.PENDING;
        head.TypeCreditNote = "P";
        head.IsProductExchange = "N";

        CreditNoteDetEntity detail = new CreditNoteDetEntity();
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnit = 1;

        CreditNoteRegisterDto request = new CreditNoteRegisterDto();
        request.Headboard = head;
        request.DetailList = List.of(detail);
        return request;
    }
}
