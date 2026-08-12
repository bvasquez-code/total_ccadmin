package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.SaleChannelEntity;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.model.factory.SaleDetailDtoFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class SaleDetailDtoFactoryTest {

    @Test
    void includesDeliveryDataInSaleDetail() {
        SaleHeadEntity head = new SaleHeadEntity();
        SaleChannelEntity channel = new SaleChannelEntity();
        SaleDeliveryEntity delivery = new SaleDeliveryEntity();

        SaleDetailDto result = SaleDetailDtoFactory.fromEntities(
                head,
                List.<SaleDetEntity>of(),
                List.<SalePaymentEntity>of(),
                List.<SaleDocumentEntity>of(),
                null,
                channel,
                delivery
        );

        assertSame(head, result.Headboard);
        assertSame(channel, result.SaleChannel);
        assertSame(delivery, result.SaleDelivery);
    }
}
