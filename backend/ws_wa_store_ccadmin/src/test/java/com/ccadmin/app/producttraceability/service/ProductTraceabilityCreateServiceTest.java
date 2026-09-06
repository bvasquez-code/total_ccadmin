package com.ccadmin.app.producttraceability.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.producttraceability.model.dto.ProductTraceabilityOperationDto;
import com.ccadmin.app.producttraceability.model.entity.ProductTraceabilityEntity;
import com.ccadmin.app.producttraceability.repository.ProductTraceabilityRepository;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductTraceabilityCreateServiceTest {

    @Mock
    private KardexRepository kardexRepository;
    @Mock
    private ProductTraceabilityRepository productTraceabilityRepository;
    @InjectMocks
    private ProductTraceabilityCreateService productTraceabilityCreateService;

    @Test
    void purchaseCreatesAvailableTechnicalLotWithDocumentedCost() {
        KardexEntity movement = movement(
                10L, PucharseConstants.KARDEX_ZONE_SOURCE,
                KardexZoneConstants.TYPE_OPERATION_ADD, 10
        );
        when(kardexRepository.findTraceabilityMovementsForUpdate(
                PucharseConstants.KARDEX_ZONE_SOURCE, "C001", "T001"
        )).thenReturn(List.of(movement));

        this.productTraceabilityCreateService.create(new ProductTraceabilityOperationDto(
                PucharseConstants.KARDEX_ZONE_SOURCE,
                "C001",
                "T001",
                null,
                Map.of(1, new BigDecimal("100.00")),
                Map.of(),
                Map.of(10L, "LT000000000000000001")
        ));

        ArgumentCaptor<ProductTraceabilityEntity> captor =
                ArgumentCaptor.forClass(ProductTraceabilityEntity.class);
        verify(productTraceabilityRepository).save(captor.capture());
        ProductTraceabilityEntity saved = captor.getValue();
        assertTrue(saved.TechnicalLot.startsWith("LT"));
        assertEquals("LT000000000000000001", saved.TechnicalLot);
        assertEquals(20, saved.TechnicalLot.length());
        assertEquals(10, saved.NumUnitAvailable);
        assertEquals(new BigDecimal("100.00"), saved.NumUnitPriceCost);
        assertEquals(new BigDecimal("1000.00"), saved.NumTotalPriceCost);
        assertEquals("A", saved.AvailabilityStatus);
    }

    @Test
    void saleSplitsTheOutputUsingAvailableLayersInFifoOrder() {
        KardexEntity movement = movement(
                20L, SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT, 15
        );
        ProductTraceabilityEntity firstLayer = layer(1L, "LT0001", 10, "100.00");
        ProductTraceabilityEntity secondLayer = layer(2L, "LT0002", 10, "120.00");

        when(kardexRepository.findTraceabilityMovementsForUpdate(
                SaleConstants.KARDEX_ZONE_SOURCE_SALE, "C001", "T001"
        )).thenReturn(List.of(movement));
        when(productTraceabilityRepository.findAvailableFromOperationForUpdate(
                SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                "C001", 1, "T001", "A001", "PR001", "0000"
        )).thenReturn(List.of());
        when(productTraceabilityRepository.findAvailableForUpdate(
                "T001", "A001", "PR001", "0000", null, null
        )).thenReturn(List.of(firstLayer, secondLayer));
        when(productTraceabilityRepository.save(any(ProductTraceabilityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.productTraceabilityCreateService.create(new ProductTraceabilityOperationDto(
                SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                "C001",
                "T001",
                null,
                Map.of(),
                Map.of(1, new BigDecimal("130.00"))
        ));

        ArgumentCaptor<ProductTraceabilityEntity> captor =
                ArgumentCaptor.forClass(ProductTraceabilityEntity.class);
        verify(productTraceabilityRepository, org.mockito.Mockito.times(4))
                .save(captor.capture());
        List<ProductTraceabilityEntity> outputs = captor.getAllValues().stream()
                .filter(item -> KardexZoneConstants.TYPE_OPERATION_SUBTRACT
                        .equals(item.TypeOperation))
                .toList();
        assertEquals(2, outputs.size());
        assertEquals(10, outputs.get(0).NumUnit);
        assertEquals("LT0001", outputs.get(0).TechnicalLot);
        assertEquals(new BigDecimal("100.00"), outputs.get(0).NumUnitPriceCost);
        assertEquals(5, outputs.get(1).NumUnit);
        assertEquals("LT0002", outputs.get(1).TechnicalLot);
        assertEquals(new BigDecimal("120.00"), outputs.get(1).NumUnitPriceCost);
        assertEquals(new BigDecimal("130.00"), outputs.get(1).NumUnitPriceSale);
        assertEquals(0, firstLayer.NumUnitAvailable);
        assertEquals("E", firstLayer.AvailabilityStatus);
        assertEquals(5, secondLayer.NumUnitAvailable);
    }

    @Test
    void manualEntryWithoutPriceUsesTheLastOutboundCost() {
        KardexEntity movement = movement(
                30L, StockMovementConstants.SOURCE_ENTRY,
                KardexZoneConstants.TYPE_OPERATION_ADD, 3
        );
        ProductTraceabilityEntity lastOutbound = layer(3L, "LT0003", 0, "87.50");
        lastOutbound.TypeOperation = KardexZoneConstants.TYPE_OPERATION_SUBTRACT;

        when(kardexRepository.findTraceabilityMovementsForUpdate(
                StockMovementConstants.SOURCE_ENTRY, "C001", "T001"
        )).thenReturn(List.of(movement));
        when(productTraceabilityRepository.findLastOutbound(
                "PR001", "0000", "T001", "A001"
        )).thenReturn(Optional.of(lastOutbound));

        this.productTraceabilityCreateService.create(new ProductTraceabilityOperationDto(
                StockMovementConstants.SOURCE_ENTRY,
                "C001",
                "T001",
                null,
                Map.of(1, BigDecimal.ZERO),
                Map.of(),
                Map.of(30L, "LT000000000000000002")
        ));

        ArgumentCaptor<ProductTraceabilityEntity> captor =
                ArgumentCaptor.forClass(ProductTraceabilityEntity.class);
        verify(productTraceabilityRepository).save(captor.capture());
        assertEquals(new BigDecimal("87.50"), captor.getValue().NumUnitPriceCost);
        verify(productTraceabilityRepository, never()).findLastPurchaseInbound(
                "PR001", "0000", "T001"
        );
    }

    @Test
    void transferReceiptPreservesTechnicalLotsAndCostsFromTheDispatch() {
        KardexEntity movement = movement(
                40L, TransferConstants.KARDEX_SOURCE_TABLE,
                KardexZoneConstants.TYPE_OPERATION_ADD, 15
        );
        ProductTraceabilityEntity firstDispatch = layer(10L, "LT0100", 0, "100.00");
        firstDispatch.TypeOperation = KardexZoneConstants.TYPE_OPERATION_SUBTRACT;
        firstDispatch.NumUnit = 10;
        ProductTraceabilityEntity secondDispatch = layer(11L, "LT0101", 0, "120.00");
        secondDispatch.TypeOperation = KardexZoneConstants.TYPE_OPERATION_SUBTRACT;
        secondDispatch.NumUnit = 5;

        when(kardexRepository.findTraceabilityMovementsForUpdate(
                TransferConstants.KARDEX_SOURCE_TABLE, "C001", "T001"
        )).thenReturn(List.of(movement));
        when(productTraceabilityRepository.findOutboundAllocationsForUpdate(
                TransferConstants.KARDEX_SOURCE_TABLE,
                "C001", 1, "PR001", "0000"
        )).thenReturn(List.of(firstDispatch, secondDispatch));

        this.productTraceabilityCreateService.create(new ProductTraceabilityOperationDto(
                TransferConstants.KARDEX_SOURCE_TABLE,
                "C001",
                "T001",
                null,
                Map.of(),
                Map.of()
        ));

        ArgumentCaptor<ProductTraceabilityEntity> captor =
                ArgumentCaptor.forClass(ProductTraceabilityEntity.class);
        verify(productTraceabilityRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());
        List<ProductTraceabilityEntity> receipts = captor.getAllValues();
        assertEquals("LT0100", receipts.get(0).TechnicalLot);
        assertEquals(10, receipts.get(0).NumUnit);
        assertEquals(10L, receipts.get(0).OriginProductTraceabilityID);
        assertEquals(new BigDecimal("100.00"), receipts.get(0).NumUnitPriceCost);
        assertEquals("LT0101", receipts.get(1).TechnicalLot);
        assertEquals(5, receipts.get(1).NumUnit);
        assertEquals(11L, receipts.get(1).OriginProductTraceabilityID);
        assertEquals(new BigDecimal("120.00"), receipts.get(1).NumUnitPriceCost);
    }

    private KardexEntity movement(
            long kardexId,
            String sourceTable,
            String typeOperation,
            int quantity
    ) {
        KardexEntity movement = KardexEntity.build(
                "C001", 1, sourceTable, typeOperation,
                "PR001", "0000", "T001", "A001", quantity,
                null, null, 1, "ADMIN"
        );
        movement.kardexID = kardexId;
        movement.CreationDate = new Date();
        return movement;
    }

    private ProductTraceabilityEntity layer(
            long id,
            String technicalLot,
            int available,
            String unitCost
    ) {
        ProductTraceabilityEntity layer = new ProductTraceabilityEntity();
        layer.ProductTraceabilityID = id;
        layer.TechnicalLot = technicalLot;
        layer.TypeOperation = KardexZoneConstants.TYPE_OPERATION_ADD;
        layer.NumUnit = 10;
        layer.NumUnitAvailable = available;
        layer.NumUnitPriceCost = new BigDecimal(unitCost);
        layer.AvailabilityStatus = available == 0 ? "E" : "A";
        layer.CreationUser = "ADMIN";
        return layer;
    }
}
