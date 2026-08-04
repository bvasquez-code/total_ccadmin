package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalePickingConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePickingLineDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDetTaxRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePickingCreateServiceTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private SaleDetRepository saleDetRepository;
    @Mock
    private SaleDetTaxRepository saleDetTaxRepository;
    @Mock
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Mock
    private SaleSearchService saleSearchService;
    @Mock
    private ProductOperationConfigShared productOperationConfigShared;
    @Mock
    private CatalogSearchShared catalogSearchShared;
    @Spy
    private SaleTaxCalculationService saleTaxCalculationService = new SaleTaxCalculationService();
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
        when(saleDetTaxRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(saleDetailTax()));
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(currentWarehouse));
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(expected);

        SaleDetailDto result = salePickingCreateService.confirm(request);

        assertEquals(expected, result);
        assertEquals("S", saleHead.IsPickingConfirmed);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetEntity>> detailCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetRepository).saveAll(detailCaptor.capture());
        List<SaleDetEntity> splitDetailList = detailCaptor.getValue();
        assertEquals(2, splitDetailList.size());
        assertEquals(1, splitDetailList.get(0).ItemNumber);
        assertEquals(4, splitDetailList.get(0).NumUnit);
        assertEquals(new BigDecimal("40.00"), splitDetailList.get(0).NumTotalPrice);
        assertEquals(new BigDecimal("32.00"), splitDetailList.get(0).NumPriceSubTotal);
        assertEquals(new BigDecimal("8.00"), splitDetailList.get(0).NumTotalTax);
        assertEquals("S", splitDetailList.get(0).IsDigital);
        assertEquals("L-01", splitDetailList.get(0).LotNumber);
        assertEquals(2, splitDetailList.get(1).ItemNumber);
        assertEquals(6, splitDetailList.get(1).NumUnit);
        assertEquals(new BigDecimal("60.00"), splitDetailList.get(1).NumTotalPrice);
        assertEquals("S", splitDetailList.get(1).IsDigital);
        assertEquals("L-02", splitDetailList.get(1).LotNumber);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetWarehouseEntity>> warehouseCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetWarehouseRepository).saveAll(warehouseCaptor.capture());
        List<SaleDetWarehouseEntity> warehouseList = warehouseCaptor.getValue();
        assertEquals(2, warehouseList.size());
        assertEquals(1, warehouseList.get(0).ItemNumber);
        assertEquals(4, warehouseList.get(0).NumUnit);
        assertEquals(2, warehouseList.get(1).ItemNumber);
        assertEquals(6, warehouseList.get(1).NumUnit);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetTaxEntity>> taxCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetTaxRepository).saveAll(taxCaptor.capture());
        List<SaleDetTaxEntity> taxList = taxCaptor.getValue();
        assertEquals(2, taxList.size());
        assertEquals(1, taxList.get(0).ItemNumber);
        assertEquals(new BigDecimal("8.00"), taxList.get(0).TaxAmount);
        assertEquals(2, taxList.get(1).ItemNumber);
        assertEquals(new BigDecimal("12.00"), taxList.get(1).TaxAmount);
        verify(saleHeadRepository).save(saleHead);
    }

    @Test
    void keepsSplitLotsNextToTheirOriginalProductAndShiftsFollowingItems() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity firstDetail = saleDetail(1, "P001", 10);
        SaleDetEntity secondDetail = saleDetail(2, "P002", 3);
        SalePickingConfirmDto request = request(
                line(1, 4, "L-01"),
                line(1, 6, "L-02"),
                line(2, 3, "L-03")
        );

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod))
                .thenReturn(List.of(firstDetail, secondDetail));
        when(saleDetTaxRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of());
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(
                        currentWarehouse(1, "P001", 10),
                        currentWarehouse(2, "P002", 3)
                ));
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(new SaleDetailDto());

        salePickingCreateService.confirm(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetEntity>> detailCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetRepository).saveAll(detailCaptor.capture());
        List<SaleDetEntity> resultList = detailCaptor.getValue();

        assertEquals(3, resultList.size());
        assertEquals(1, resultList.get(0).ItemNumber);
        assertEquals("P001", resultList.get(0).ProductCod);
        assertEquals("L-01", resultList.get(0).LotNumber);
        assertEquals(2, resultList.get(1).ItemNumber);
        assertEquals("P001", resultList.get(1).ProductCod);
        assertEquals("L-02", resultList.get(1).LotNumber);
        assertEquals(3, resultList.get(2).ItemNumber);
        assertEquals("P002", resultList.get(2).ProductCod);
        assertEquals("L-03", resultList.get(2).LotNumber);

        InOrder persistenceOrder = inOrder(
                saleDetTaxRepository,
                saleDetWarehouseRepository,
                saleDetRepository
        );
        persistenceOrder.verify(saleDetTaxRepository).deleteBySaleCodNative(saleHead.SaleCod);
        persistenceOrder.verify(saleDetWarehouseRepository).deleteBySaleCodNative(saleHead.SaleCod);
        persistenceOrder.verify(saleDetRepository).deleteBySaleCodNative(saleHead.SaleCod);
        persistenceOrder.verify(saleDetRepository).saveAll(anyList());
        persistenceOrder.verify(saleDetWarehouseRepository).saveAll(anyList());
        persistenceOrder.verify(saleDetTaxRepository).saveAll(anyList());
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
    void confirmsPickingWithoutLotOrExpirationMetadata() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity saleDetail = saleDetail(10);
        SalePickingConfirmDto request = request(line(10, null));

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(saleDetail));
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(currentWarehouse(10)));
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(new SaleDetailDto());

        salePickingCreateService.confirm(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetWarehouseEntity>> allocationCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetWarehouseRepository).saveAll(allocationCaptor.capture());
        assertNull(allocationCaptor.getValue().get(0).LotNumber);
        assertNull(allocationCaptor.getValue().get(0).ExpirationDate);
    }

    @Test
    void confirmsOnlyPickedProductsWhenFullPickingIsNotMandatory() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity pickedDetail = saleDetail(1, "P001", 10);
        SaleDetEntity unpickedDetail = saleDetail(2, "P002", 3);
        SalePickingConfirmDto request = request(
                line(1, 4, "L-01"),
                line(1, 6, "L-02")
        );

        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_MANDATORY_PICKING
        )).thenReturn(false);
        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod))
                .thenReturn(List.of(pickedDetail, unpickedDetail));
        when(saleDetTaxRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of());
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(
                        currentWarehouse(1, "P001", 10),
                        currentWarehouse(2, "P002", 3)
                ));
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(new SaleDetailDto());

        salePickingCreateService.confirm(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetEntity>> detailCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetRepository).saveAll(detailCaptor.capture());
        List<SaleDetEntity> detailList = detailCaptor.getValue();
        assertEquals(3, detailList.size());
        assertEquals("P001", detailList.get(0).ProductCod);
        assertEquals("P001", detailList.get(1).ProductCod);
        assertEquals("P002", detailList.get(2).ProductCod);
        assertEquals(3, detailList.get(2).ItemNumber);
        assertEquals(3, detailList.get(2).NumUnit);
        assertNull(detailList.get(2).LotNumber);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleDetWarehouseEntity>> warehouseCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleDetWarehouseRepository).saveAll(warehouseCaptor.capture());
        assertEquals(3, warehouseCaptor.getValue().size());
        assertEquals("P002", warehouseCaptor.getValue().get(2).ProductCod);
        assertEquals(3, warehouseCaptor.getValue().get(2).ItemNumber);
        assertEquals(3, warehouseCaptor.getValue().get(2).NumUnit);
    }

    @Test
    void rejectsPartialPickingWhenFullPickingIsMandatory() {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetEntity firstDetail = saleDetail(1, "P001", 10);
        SaleDetEntity secondDetail = saleDetail(2, "P002", 3);

        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_MANDATORY_PICKING
        )).thenReturn(true);
        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetRepository.findBySaleCod(saleHead.SaleCod))
                .thenReturn(List.of(firstDetail, secondDetail));
        when(saleDetTaxRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of());
        when(saleDetWarehouseRepository.findBySaleCodForUpdate(saleHead.SaleCod))
                .thenReturn(List.of(
                        currentWarehouse(1, "P001", 10),
                        currentWarehouse(2, "P002", 3)
                ));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> salePickingCreateService.confirm(request(line(1, 10, "L-01")))
        );

        assertEquals("Falta pickear el item 2", exception.getMessage());
        verify(saleDetRepository, never()).saveAll(anyList());
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
        return saleDetail(1, "P001", quantity);
    }

    private SaleDetEntity saleDetail(int itemNumber, String productCod, int quantity) {
        SaleDetEntity detail = new SaleDetEntity();
        detail.SaleCod = "ST001";
        detail.ItemNumber = itemNumber;
        detail.ProductCod = productCod;
        detail.Variant = "0000";
        detail.NumUnit = quantity;
        detail.NumUnitPrice = new BigDecimal("10.00");
        detail.NumDiscount = BigDecimal.ZERO.setScale(2);
        detail.NumUnitPriceSale = new BigDecimal("10.00");
        detail.NumTotalPrice = new BigDecimal("100.00");
        detail.NumPriceSubTotal = new BigDecimal("80.00");
        detail.NumTotalTax = new BigDecimal("20.00");
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        detail.IsDigital = "S";
        return detail;
    }

    private SaleDetWarehouseEntity currentWarehouse(int quantity) {
        return currentWarehouse(1, "P001", quantity);
    }

    private SaleDetWarehouseEntity currentWarehouse(int itemNumber, String productCod, int quantity) {
        SaleDetWarehouseEntity detail = new SaleDetWarehouseEntity();
        detail.SaleCod = "ST001";
        detail.ItemNumber = itemNumber;
        detail.ProductCod = productCod;
        detail.Variant = "0000";
        detail.WarehouseCod = "W001";
        detail.NumUnit = quantity;
        return detail;
    }

    private SaleDetTaxEntity saleDetailTax() {
        SaleDetTaxEntity tax = new SaleDetTaxEntity();
        tax.SaleCod = "ST001";
        tax.ItemNumber = 1;
        tax.TaxLineNumber = 1;
        tax.TaxCod = "IGV";
        tax.TaxName = "IGV";
        tax.TaxCalculationType = "P";
        tax.IsInformative = "N";
        tax.TaxRateValue = new BigDecimal("18.00");
        tax.FixedUnitAmount = BigDecimal.ZERO.setScale(4);
        tax.TaxBaseAmount = new BigDecimal("80.00");
        tax.TaxQuantity = new BigDecimal("10.0000");
        tax.TaxAmount = new BigDecimal("20.00");
        tax.CalculationOrder = 1;
        return tax;
    }

    private SalePickingLineDto line(int quantity, String lotNumber) {
        return line(1, quantity, lotNumber);
    }

    private SalePickingLineDto line(int itemNumber, int quantity, String lotNumber) {
        SalePickingLineDto line = new SalePickingLineDto();
        line.ItemNumber = itemNumber;
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
