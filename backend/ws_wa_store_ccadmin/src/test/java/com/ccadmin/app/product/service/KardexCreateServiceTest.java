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
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @InjectMocks
    private KardexCreateService kardexCreateService;

    @Test
    void saleMovementsAreChainedValidatedAndSavedAsOneStockOperation() throws Exception {
        ProductInfoEntity productStock = productStock();
        ProductInfoWarehouseEntity warehouseStock = warehouseStock();
        KardexEntity lastKardex = new KardexEntity();
        lastKardex.NumStockAfter = 10;
        KardexZoneEntity lastPhysical = zoneBalance(KardexZoneConstants.ZONE_PHYSICAL, 8);
        KardexZoneEntity lastReserved = zoneBalance(KardexZoneConstants.ZONE_RESERVED, 2);

        when(productInfoRepository.findByIdForUpdate("PR001", "0000", "T001"))
                .thenReturn(Optional.of(productStock));
        when(productInfoWarehouseRepository.findByIdForUpdate("PR001", "0000", "A001"))
                .thenReturn(Optional.of(warehouseStock));
        when(kardexRepository.findLastMovement("PR001", "0000", "A001", "T001"))
                .thenReturn(lastKardex);
        when(kardexZoneRepository.findLastMovement(
                "PR001", "0000", "T001", "A001", KardexZoneConstants.ZONE_PHYSICAL
        )).thenReturn(lastPhysical);
        when(kardexZoneRepository.findLastMovement(
                "PR001", "0000", "T001", "A001", KardexZoneConstants.ZONE_RESERVED
        )).thenReturn(lastReserved);
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

    private KardexZoneEntity zoneBalance(String zone, int balance) {
        KardexZoneEntity movement = new KardexZoneEntity();
        movement.ZoneStockMoved = zone;
        movement.NumZoneStockAfter = balance;
        return movement;
    }
}
