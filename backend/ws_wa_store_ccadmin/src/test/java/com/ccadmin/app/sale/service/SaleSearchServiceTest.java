package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.CommercialChannelEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.CommercialChannelRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleSearchServiceTest {

    @Mock private SaleHeadRepository saleHeadRepository;
    @Mock private CommercialChannelRepository commercialChannelRepository;
    @Mock private ClientShared clientShared;
    @InjectMocks private SaleSearchService saleSearchService;

    @Test
    void findsOnlySalesFromRequestedChannelAndLoadsClientsInBatch() {
        CommercialChannelEntity webChannel = new CommercialChannelEntity();
        webChannel.ChannelCod = SaleConstants.COMMERCIAL_CHANNEL_WEB;

        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        sale.StoreCod = "T001";
        sale.ClientCod = "CL001";
        sale.CurrencyCod = "PEN";

        ClientEntity client = new ClientEntity();
        client.ClientCod = "CL001";

        when(commercialChannelRepository.findActiveByChannelCod(SaleConstants.COMMERCIAL_CHANNEL_WEB))
                .thenReturn(Optional.of(webChannel));
        when(saleHeadRepository.findByStoreAndChannel(
                "cliente",
                "T001",
                SaleConstants.COMMERCIAL_CHANNEL_WEB,
                10,
                10
        )).thenReturn(List.of(sale));
        when(saleHeadRepository.countByStoreAndChannel(
                "cliente",
                "T001",
                SaleConstants.COMMERCIAL_CHANNEL_WEB
        )).thenReturn(11);
        when(clientShared.findAllById(List.of("CL001"))).thenReturn(List.of(client));

        ResponsePageSearchT<SaleHeadEntity> response = saleSearchService.findAll(
                " cliente ",
                2,
                "T001",
                SaleConstants.COMMERCIAL_CHANNEL_WEB
        );

        assertEquals(11, response.TotalResult);
        assertEquals(2, response.TotalPages);
        assertEquals(2, response.Page);
        assertSame(client, response.resultSearch.get(0).Client);
    }

    @Test
    void rejectsUnknownCommercialChannelBeforeQueryingSales() {
        when(commercialChannelRepository.findActiveByChannelCod("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> saleSearchService.findAll("", 1, "T001", "UNKNOWN")
        );

        verify(saleHeadRepository, never()).findByStoreAndChannel(
                "",
                "T001",
                "UNKNOWN",
                0,
                10
        );
    }
}
