package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.dto.SaleDeliveryStatusChangeDto;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleDeliveryCreateServiceTest {

    @Mock
    private VirtualCartRepository virtualCartRepository;
    @Mock
    private SaleDeliveryRepository saleDeliveryRepository;
    @Mock
    private SaleHeadRepository saleHeadRepository;

    @Test
    void convertsCartDeliverySnapshotWhenSaleIsCreated() {
        VirtualCartEntity cart = new VirtualCartEntity();
        cart.PresaleCod = "PS001";
        cart.CartData = """
                {"Delivery":{"DeliveryTypeCod":"DELIVERY","Names":"Cliente",\
                "Phone":"999999999","Address":"Calle 1","Latitude":-6.77,"Longitude":-79.84}}
                """;
        when(virtualCartRepository.findConvertedByPresaleCod("PS001"))
                .thenReturn(Optional.of(cart));
        SaleDeliveryCreateService service = new SaleDeliveryCreateService(
                virtualCartRepository,
                saleDeliveryRepository,
                new ObjectMapper()
        );

        service.createFromConvertedCart("PS001", "ST001", "USER01");

        ArgumentCaptor<SaleDeliveryEntity> deliveryCaptor =
                ArgumentCaptor.forClass(SaleDeliveryEntity.class);
        verify(saleDeliveryRepository).save(deliveryCaptor.capture());
        assertEquals("ST001", deliveryCaptor.getValue().SaleCod);
        assertEquals(SaleConstants.DELIVERY_STATUS_PENDING, deliveryCaptor.getValue().DeliveryStatus);
        assertEquals("DELIVERY", deliveryCaptor.getValue().DeliveryTypeCod);
        assertEquals("ST001", cart.SaleCod);
        verify(virtualCartRepository).save(cart);
    }

    @Test
    void startsPreparationForPaidWebOrder() throws Exception {
        SaleDeliveryCreateService service = service();
        SaleHeadEntity saleHead = webSale(SaleConstants.PENDING, "S");
        SaleDeliveryEntity delivery = delivery(
                SaleConstants.DELIVERY_STATUS_PENDING,
                SaleConstants.DELIVERY_TYPE_AUTOMATIC
        );
        SaleDeliveryStatusChangeDto request = request(SaleConstants.DELIVERY_STATUS_PREPARING, "");
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(saleHead));
        when(saleDeliveryRepository.findActiveBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(delivery));
        when(saleDeliveryRepository.save(delivery)).thenReturn(delivery);

        SaleDeliveryEntity result = service.changeStatus(request);

        assertEquals(SaleConstants.DELIVERY_STATUS_PREPARING, result.DeliveryStatus);
        verify(saleDeliveryRepository).save(delivery);
    }

    @Test
    void rejectsStartingPreparationWhenOrderIsNotPaid() {
        SaleDeliveryCreateService service = service();
        SaleHeadEntity saleHead = webSale(SaleConstants.PENDING, "N");
        SaleDeliveryEntity delivery = delivery(
                SaleConstants.DELIVERY_STATUS_PENDING,
                SaleConstants.DELIVERY_TYPE_AUTOMATIC
        );
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(saleHead));
        when(saleDeliveryRepository.findActiveBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(delivery));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> service.changeStatus(request(SaleConstants.DELIVERY_STATUS_PREPARING, ""))
        );

        assertEquals(
                "El pedido debe estar pagado antes de iniciar su preparacion",
                exception.getMessage()
        );
    }

    @Test
    void failedDeliveryRequiresAReason() {
        SaleDeliveryCreateService service = service();
        SaleHeadEntity saleHead = webSale(SaleConstants.CONFIRMED, "S");
        saleHead.HasFiscalDocument = "S";
        SaleDeliveryEntity delivery = delivery(
                SaleConstants.DELIVERY_STATUS_DISPATCHED,
                SaleConstants.DELIVERY_TYPE_AUTOMATIC
        );
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(saleHead));
        when(saleDeliveryRepository.findActiveBySaleCodForUpdate("ST001"))
                .thenReturn(Optional.of(delivery));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> service.changeStatus(request(SaleConstants.DELIVERY_STATUS_FAILED, ""))
        );

        assertEquals("Debe indicar el motivo de la entrega fallida", exception.getMessage());
    }

    private SaleDeliveryCreateService service() {
        SaleDeliveryCreateService service = new SaleDeliveryCreateService(
                virtualCartRepository,
                saleDeliveryRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "saleHeadRepository", saleHeadRepository);
        return service;
    }

    private SaleHeadEntity webSale(String saleStatus, String isPaid) {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST001";
        saleHead.SaleStatus = saleStatus;
        saleHead.IsPaid = isPaid;
        return saleHead;
    }

    private SaleDeliveryEntity delivery(String status, String type) {
        SaleDeliveryEntity delivery = new SaleDeliveryEntity();
        delivery.SaleCod = "ST001";
        delivery.DeliveryStatus = status;
        delivery.DeliveryTypeCod = type;
        return delivery;
    }

    private SaleDeliveryStatusChangeDto request(String targetStatus, String commenter) {
        SaleDeliveryStatusChangeDto request = new SaleDeliveryStatusChangeDto();
        request.SaleCod = "ST001";
        request.TargetStatus = targetStatus;
        request.Commenter = commenter;
        return request;
    }
}
