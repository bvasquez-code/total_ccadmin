package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.shared.ProductFindCreateShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.producttraceability.event.ProductTraceabilityConfirmedOperationEvent;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexCreateServiceTest {

    @Mock
    private KardexRepository kardexRepository;
    @Mock
    private KardexZoneRepository kardexZoneRepository;
    @Mock
    private ProductInfoRepository productInfoRepository;
    @Mock
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Mock
    private ProductFindCreateShared productFindCreateShared;
    @Mock
    private ProductOperationConfigShared productOperationConfigShared;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks
    private KardexCreateService kardexCreateService;

    @Test
    void saleMovementsUseInfoTablesAndAreSavedAsOneStockOperation() throws Exception {
        ProductInfoEntity productStock = productStock();
        ProductInfoWarehouseEntity warehouseStock = warehouseStock();

        when(productInfoRepository.findByIdForUpdate("PR001", "0000", "T001"))
                .thenReturn(Optional.of(productStock));
        when(productInfoWarehouseRepository.findByIdForUpdate("PR001", "0000", "A001"))
                .thenReturn(Optional.of(warehouseStock));
        when(kardexRepository.save(any(KardexEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(kardexZoneRepository.save(any(KardexZoneEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleHeadEntity head = new SaleHeadEntity();
        head.SaleCod = "V001";
        head.PresaleCod = "P001";
        head.StoreCod = "T001";
        SaleDetWarehouseEntity detail = new SaleDetWarehouseEntity();
        detail.SaleCod = head.SaleCod;
        detail.ItemNumber = 1;
        detail.ProductCod = "PR001";
        detail.Variant = "0000";
        detail.WarehouseCod = "A001";
        detail.NumUnit = 2;

        List<KardexEntity> kardexList = this.kardexCreateService.buildSaleConfirmation(
                head, List.of(detail), "ADMIN"
        );
        List<KardexZoneEntity> zoneList = KardexZoneEntity.buildSaleConfirmation(
                head, detail, "ADMIN"
        );

        this.kardexCreateService.saveAll(kardexList, zoneList);

        assertEquals(10, kardexList.get(0).NumStockBefore);
        assertEquals(8, kardexList.get(0).NumStockAfter);
        assertEquals(2, zoneList.get(0).NumZoneStockBefore);
        assertEquals(0, zoneList.get(0).NumZoneStockAfter);
        assertEquals(8, zoneList.get(1).NumZoneStockBefore);
        assertEquals(10, zoneList.get(1).NumZoneStockAfter);
        assertEquals(10, zoneList.get(2).NumZoneStockBefore);
        assertEquals(8, zoneList.get(2).NumZoneStockAfter);
        assertEquals(8, warehouseStock.NumPhysicalStock);
        assertEquals(0, warehouseStock.NumReservedStock);
        assertEquals(8, warehouseStock.NumTotalStock);
        assertEquals(8, productStock.NumTotalStock);
        verify(kardexRepository).save(kardexList.get(0));
        verify(kardexZoneRepository, times(3)).save(any(KardexZoneEntity.class));
        verify(applicationEventPublisher).publishEvent(
                new ProductTraceabilityConfirmedOperationEvent("sale_head", "V001", "T001")
        );
        verify(kardexRepository, never()).findLastMovement(
                "PR001", "0000", "A001", "T001"
        );
        verify(kardexZoneRepository, never()).findLastMovement(
                "PR001", "0000", "T001", "A001", KardexZoneConstants.ZONE_PHYSICAL
        );
    }

    @Test
    void validatesPresaleReservationAgainstTheSumOfPickedLots() throws Exception {
        SaleHeadEntity head = new SaleHeadEntity();
        head.SaleCod = "V002";
        head.PresaleCod = "P002";
        head.StoreCod = "T001";

        SaleDetWarehouseEntity firstLot = saleWarehouseAllocation(head.SaleCod, 1, 4, "L-01");
        SaleDetWarehouseEntity secondLot = saleWarehouseAllocation(head.SaleCod, 2, 6, "L-02");
        KardexZoneEntity physicalReservation = reservationMovement(
                KardexZoneConstants.ZONE_PHYSICAL,
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT,
                10
        );
        KardexZoneEntity reservedReservation = reservationMovement(
                KardexZoneConstants.ZONE_RESERVED,
                KardexZoneConstants.TYPE_OPERATION_ADD,
                10
        );

        when(kardexZoneRepository.findByOperationEvent(
                "presale_head", head.PresaleCod, "PRESALE_RESERVATION"
        )).thenReturn(List.of(physicalReservation, reservedReservation));

        List<KardexZoneEntity> movementList = kardexCreateService.buildZoneSaleConfirmation(
                head, List.of(firstLot, secondLot), "ADMIN"
        );

        assertEquals(6, movementList.size());
        assertEquals(4, movementList.get(0).NumStockMoved);
        assertEquals("L-01", movementList.get(0).LotNumber);
        assertEquals(6, movementList.get(3).NumStockMoved);
        assertEquals("L-02", movementList.get(3).LotNumber);
        verify(kardexZoneRepository, times(1)).findByOperationEvent(
                "presale_head", head.PresaleCod, "PRESALE_RESERVATION"
        );
    }

    @Test
    void digitalProductsNeverPersistKardexOrModifyStock() {
        KardexEntity movement = new KardexEntity();
        movement.ProductCod = "DIG001";
        movement.StoreCod = "T001";
        KardexZoneEntity zoneMovement = new KardexZoneEntity();
        zoneMovement.ProductCod = "DIG001";
        zoneMovement.StoreCod = "T001";

        when(productOperationConfigShared.isDigital("DIG001", "T001")).thenReturn(true);

        List<KardexEntity> result = this.kardexCreateService.saveAll(
                List.of(movement), List.of(zoneMovement)
        );

        assertEquals(List.of(), result);
        verifyNoInteractions(
                productInfoRepository,
                productInfoWarehouseRepository,
                kardexRepository,
                kardexZoneRepository
        );
    }

    private SaleDetWarehouseEntity saleWarehouseAllocation(
            String saleCod,
            int itemNumber,
            int quantity,
            String lotNumber
    ) {
        SaleDetWarehouseEntity detail = new SaleDetWarehouseEntity();
        detail.SaleCod = saleCod;
        detail.ItemNumber = itemNumber;
        detail.ProductCod = "PR001";
        detail.Variant = "0000";
        detail.WarehouseCod = "A001";
        detail.NumUnit = quantity;
        detail.LotNumber = lotNumber;
        return detail;
    }

    private KardexZoneEntity reservationMovement(String zone, String operation, int quantity) {
        KardexZoneEntity movement = new KardexZoneEntity();
        movement.ProductCod = "PR001";
        movement.Variant = "0000";
        movement.StoreCod = "T001";
        movement.WarehouseCod = "A001";
        movement.ZoneStockMoved = zone;
        movement.TypeOperation = operation;
        movement.NumStockMoved = quantity;
        return movement;
    }

    private ProductInfoEntity productStock() {
        ProductInfoEntity stock = new ProductInfoEntity();
        stock.ProductCod = "PR001";
        stock.Variant = "0000";
        stock.StoreCod = "T001";
        stock.NumPhysicalStock = 8;
        stock.NumReservedStock = 2;
        stock.NumTotalStock = 10;
        stock.NumDigitalStock = 8;
        return stock;
    }

    private ProductInfoWarehouseEntity warehouseStock() {
        ProductInfoWarehouseEntity stock = new ProductInfoWarehouseEntity();
        stock.ProductCod = "PR001";
        stock.Variant = "0000";
        stock.WarehouseCod = "A001";
        stock.NumPhysicalStock = 8;
        stock.NumReservedStock = 2;
        stock.NumTotalStock = 10;
        stock.NumDigitalStock = 8;
        return stock;
    }

}
