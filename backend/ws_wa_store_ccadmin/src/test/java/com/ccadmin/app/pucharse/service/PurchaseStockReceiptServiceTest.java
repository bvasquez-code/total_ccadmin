package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseStockReceiptServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @Mock
    private KardexShared kardexShared;
    @InjectMocks
    private PurchaseStockReceiptService receiptService;

    @Test
    void shouldAddReceivedStockToPhysicalZoneAndTotalKardex() throws Exception {
        PucharseHeadEntity purchase = purchase();
        PucharseDetDeliveryEntity delivery = delivery(1, 10);
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of(new KardexZoneEntity()));
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);
        ArgumentCaptor<List<KardexEntity>> kardexCaptor = ArgumentCaptor.forClass(List.class);

        List<KardexEntity> result = this.receiptService.receive(purchase, List.of(delivery), "USER01");

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.SourceTable).isEqualTo(PucharseConstants.KARDEX_ZONE_SOURCE);
        assertThat(operation.MovementEvent).isEqualTo(PucharseConstants.KARDEX_ZONE_EVENT_RECEIPT);
        assertThat(operation.MovementList).hasSize(1);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(10);

        verify(this.kardexShared).saveAllLedgerOnly(kardexCaptor.capture());
        KardexEntity kardex = kardexCaptor.getValue().get(0);
        assertThat(kardex.TypeOperation).isEqualTo("S");
        assertThat(kardex.NumStockMoved).isEqualTo(10);
        assertThat(kardex.LotNumber).isEqualTo("LOT-01");
        assertThat(result).containsExactly(kardex);
    }

    @Test
    void shouldNotDuplicateKardexWhenReceiptEventAlreadyExists() throws Exception {
        PucharseHeadEntity purchase = purchase();
        PucharseDetDeliveryEntity delivery = delivery(1, 10);
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of());

        List<KardexEntity> result = this.receiptService.receive(purchase, List.of(delivery), "USER01");

        assertThat(result).isEmpty();
        verify(this.kardexShared, never()).findLastMovement(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(this.kardexShared, never()).saveAllLedgerOnly(anyList());
    }

    @Test
    void shouldChainKardexBalancesForRepeatedProductInSameReception() throws Exception {
        PucharseHeadEntity purchase = purchase();
        PucharseDetDeliveryEntity first = delivery(1, 10);
        PucharseDetDeliveryEntity second = delivery(2, 5);
        KardexEntity previous = new KardexEntity();
        previous.NumStockAfter = 20;
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of(new KardexZoneEntity()));
        when(this.kardexShared.findLastMovement("P001", "0000", "W001", "S001"))
                .thenReturn(previous);

        List<KardexEntity> result = this.receiptService.receive(
                purchase,
                List.of(first, second),
                "USER01"
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).NumStockBefore).isEqualTo(20);
        assertThat(result.get(0).NumStockAfter).isEqualTo(30);
        assertThat(result.get(1).NumStockBefore).isEqualTo(30);
        assertThat(result.get(1).NumStockAfter).isEqualTo(35);
    }

    private PucharseHeadEntity purchase() {
        PucharseHeadEntity purchase = new PucharseHeadEntity();
        purchase.PucharseCod = "PC001";
        purchase.StoreCod = "S001";
        return purchase;
    }

    private PucharseDetDeliveryEntity delivery(int itemNumber, int quantity) {
        PucharseDetDeliveryEntity delivery = new PucharseDetDeliveryEntity();
        delivery.PucharseCod = "PC001";
        delivery.ItemNumber = itemNumber;
        delivery.ProductCod = "P001";
        delivery.Variant = "0000";
        delivery.WarehouseCod = "W001";
        delivery.NumUnit = quantity;
        delivery.LotNumber = "LOT-01";
        return delivery;
    }
}
