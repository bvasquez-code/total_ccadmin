package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.KardexZoneException;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.shared.ProductFindCreateShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexZoneServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;
    @Mock
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Mock
    private KardexZoneRepository kardexZoneRepository;
    @Mock
    private ProductFindCreateShared productFindCreateShared;
    @InjectMocks
    private KardexZoneService kardexZoneService;

    @Test
    void shouldMovePhysicalStockToReservedWithoutChangingTotalStock() {
        ProductInfoEntity productInfo = productInfo(100, 0, 0, 100);
        ProductInfoWarehouseEntity warehouse = warehouse(100, 0, 0, 100);
        KardexZoneOperationDto operation = presaleReservation();
        this.mockLockedStock(productInfo, warehouse);

        List<KardexZoneEntity> result = this.kardexZoneService.apply(operation, "USER01");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(result.get(0).TypeOperation).isEqualTo(KardexZoneConstants.TYPE_OPERATION_SUBTRACT);
        assertThat(result.get(0).NumZoneStockBefore).isEqualTo(100);
        assertThat(result.get(0).NumZoneStockAfter).isEqualTo(90);
        assertThat(result.get(1).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_RESERVED);
        assertThat(result.get(1).TypeOperation).isEqualTo(KardexZoneConstants.TYPE_OPERATION_ADD);
        assertThat(result.get(1).NumZoneStockBefore).isZero();
        assertThat(result.get(1).NumZoneStockAfter).isEqualTo(10);
        assertThat(warehouse.NumPhysicalStock).isEqualTo(90);
        assertThat(warehouse.NumReservedStock).isEqualTo(10);
        assertThat(warehouse.NumTotalStock).isEqualTo(100);
        assertThat(productInfo.NumPhysicalStock).isEqualTo(90);
        assertThat(productInfo.NumReservedStock).isEqualTo(10);
        assertThat(productInfo.NumTotalStock).isEqualTo(100);
        verify(this.kardexZoneRepository).saveAll(anyList());
    }

    @Test
    void shouldIgnoreAnEventThatWasAlreadyApplied() {
        ProductInfoEntity productInfo = productInfo(100, 0, 0, 100);
        ProductInfoWarehouseEntity warehouse = warehouse(100, 0, 0, 100);
        KardexZoneOperationDto operation = presaleReservation();
        this.mockLockedStock(productInfo, warehouse);
        when(this.kardexZoneRepository.countByEvent(
                operation.SourceTable,
                operation.OperationCod,
                operation.ItemNumber,
                operation.MovementEvent
        )).thenReturn(2);

        List<KardexZoneEntity> result = this.kardexZoneService.apply(operation, "USER01");

        assertThat(result).isEmpty();
        assertThat(productInfo.NumPhysicalStock).isEqualTo(100);
        assertThat(warehouse.NumPhysicalStock).isEqualTo(100);
        verify(this.kardexZoneRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldChainRepeatedMovementsInTheSameZone() {
        ProductInfoEntity productInfo = productInfo(90, 0, 10, 100);
        ProductInfoWarehouseEntity warehouse = warehouse(90, 0, 10, 100);
        KardexZoneOperationDto operation = presaleReservation();
        operation.OperationCod = "SL001";
        operation.SourceTable = "sale_head";
        operation.MovementEvent = "CONFIRMATION";
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_RESERVED, -10),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, 10),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, -10)
        );
        this.mockLockedStock(productInfo, warehouse);

        List<KardexZoneEntity> result = this.kardexZoneService.apply(operation, "USER01");

        assertThat(result).hasSize(3);
        assertThat(result.get(1).NumZoneStockBefore).isEqualTo(90);
        assertThat(result.get(1).NumZoneStockAfter).isEqualTo(100);
        assertThat(result.get(2).NumZoneStockBefore).isEqualTo(100);
        assertThat(result.get(2).NumZoneStockAfter).isEqualTo(90);
        assertThat(warehouse.NumPhysicalStock).isEqualTo(90);
        assertThat(warehouse.NumReservedStock).isZero();
        assertThat(warehouse.NumTotalStock).isEqualTo(90);
    }

    @Test
    void shouldRejectMovementWhenZoneStockIsInsufficient() {
        ProductInfoEntity productInfo = productInfo(5, 0, 0, 5);
        ProductInfoWarehouseEntity warehouse = warehouse(5, 0, 0, 5);
        KardexZoneOperationDto operation = presaleReservation();
        this.mockLockedStock(productInfo, warehouse);

        assertThatThrownBy(() -> this.kardexZoneService.apply(operation, "USER01"))
                .isInstanceOf(KardexZoneException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(this.kardexZoneRepository, never()).saveAll(anyList());
    }

    private void mockLockedStock(ProductInfoEntity productInfo, ProductInfoWarehouseEntity warehouse) {
        when(this.productInfoRepository.findByIdForUpdate("P001", "0000", "S001"))
                .thenReturn(Optional.of(productInfo));
        when(this.productInfoWarehouseRepository.findByIdForUpdate("P001", "0000", "W001"))
                .thenReturn(Optional.of(warehouse));
    }

    private KardexZoneOperationDto presaleReservation() {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = "PS001";
        operation.ItemNumber = 1;
        operation.SourceTable = "presale_head";
        operation.MovementEvent = "RESERVATION";
        operation.ProductCod = "P001";
        operation.Variant = "0000";
        operation.StoreCod = "S001";
        operation.WarehouseCod = "W001";
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, -10),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_RESERVED, 10)
        );
        return operation;
    }

    private ProductInfoEntity productInfo(int physical, int unavailable, int reserved, int total) {
        ProductInfoEntity entity = new ProductInfoEntity();
        entity.ProductCod = "P001";
        entity.Variant = "0000";
        entity.StoreCod = "S001";
        entity.NumPhysicalStock = physical;
        entity.NumDigitalStock = physical;
        entity.NumUnavailableStock = unavailable;
        entity.NumReservedStock = reserved;
        entity.NumTotalStock = total;
        return entity;
    }

    private ProductInfoWarehouseEntity warehouse(int physical, int unavailable, int reserved, int total) {
        ProductInfoWarehouseEntity entity = new ProductInfoWarehouseEntity();
        entity.ProductCod = "P001";
        entity.Variant = "0000";
        entity.WarehouseCod = "W001";
        entity.NumPhysicalStock = physical;
        entity.NumDigitalStock = physical;
        entity.NumUnavailableStock = unavailable;
        entity.NumReservedStock = reserved;
        entity.NumTotalStock = total;
        return entity;
    }
}
