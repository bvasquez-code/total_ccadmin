package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalePickingConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePickingLineDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePickingCreateServiceTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private SaleDetRepository saleDetRepository;
    @Mock
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Mock
    private SaleSearchService saleSearchService;
    @Mock
    private ProductOperationConfigShared productOperationConfigShared;
    @InjectMocks
    private SalePickingCreateService salePickingCreateService;

    @Test
    void confirmsAllLotAllocationsInOneOperation() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity saleDetail = saleDetail(10);
        SaleDetWarehouseEntity currentWarehouse = currentWarehouse(10);
        SalePickingConfirmDto request = request(line(4, "L-01"), line(6, "L-02"));
        SaleDetailDto expected = new SaleDetailDto();

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(saleDetail));
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(currentWarehouse));
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(expected);

        SaleDetailDto result = salePickingCreateService.confirm(request);

        assertEquals(expected, result);
        assertEquals("S", saleHead.IsPickingConfirmed);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetWarehouseEntity>> allocationCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetWarehouseRepository).saveAll(allocationCaptor.capture());
        List<SaleDetWarehouseEntity> allocationList = allocationCaptor.getValue();
        assertEquals(2, allocationList.size());
        assertEquals(1, allocationList.get(0).AllocationNumber);
        assertEquals(4, allocationList.get(0).NumUnit);
        assertEquals("L-01", allocationList.get(0).LotNumber);
        assertEquals(2, allocationList.get(1).AllocationNumber);
        assertEquals(6, allocationList.get(1).NumUnit);
        assertEquals("L-02", allocationList.get(1).LotNumber);
        verify(saleHeadRepository).save(saleHead);
    }

    @Test
    void rejectsPickingWhenTotalIsLowerThanSaleQuantity() {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity saleDetail = saleDetail(10);
        SalePickingConfirmDto request = request(line(9, "L-01"));

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(saleDetail));
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(currentWarehouse(10)));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> salePickingCreateService.confirm(request)
        );

        assertEquals("El item 1 requiere 10 unidades y se pickearon 9", exception.getMessage());
        verify(saleDetWarehouseRepository, never()).saveAll(anyList());
        verify(saleHeadRepository, never()).save(saleHead);
    }

    @Test
    void rejectsSecondPickingConfirmation() {
        SaleHeadEntity saleHead = pendingSale();
        saleHead.IsPickingConfirmed = "S";
        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> salePickingCreateService.confirm(request(line(10, "L-01")))
        );

        assertEquals("El pickeo de la venta ya fue confirmado", exception.getMessage());
        verify(saleDetRepository, never()).findBySaleCod(saleHead.SaleCod);
    }

    private SaleHeadEntity pendingSale() {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST001";
        saleHead.SaleStatus = SaleConstants.PENDING;
        saleHead.IsPickingConfirmed = "N";
        return saleHead;
    }

    private SaleDetEntity saleDetail(int quantity) {
        SaleDetEntity detail = new SaleDetEntity();
        detail.SaleCod = "ST001";
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnit = quantity;
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        return detail;
    }

    private SaleDetWarehouseEntity currentWarehouse(int quantity) {
        SaleDetWarehouseEntity detail = new SaleDetWarehouseEntity();
        detail.SaleCod = "ST001";
        detail.ItemNumber = 1;
        detail.AllocationNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.WarehouseCod = "W001";
        detail.NumUnit = quantity;
        return detail;
    }

    private SalePickingLineDto line(int quantity, String lotNumber) {
        SalePickingLineDto line = new SalePickingLineDto();
        line.ItemNumber = 1;
        line.NumUnit = quantity;
        line.LotNumber = lotNumber;
        return line;
    }

    private SalePickingConfirmDto request(SalePickingLineDto... lineList) {
        SalePickingConfirmDto request = new SalePickingConfirmDto();
        request.SaleCod = "ST001";
        request.DetailList = List.of(lineList);
        return request;
    }
}
