package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.KardexExcepcion;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KardexMovementBuildTest {

    @Test
    void kardexBuildDoesNotCalculateBalanceUntilPreviousMovementIsApplied() {
        KardexEntity movement = KardexEntity.build(
                "V001", 1, "sale_head", "R",
                "P001", "0000", "T001", "A001",
                4, null, null, 1, "ADMIN"
        );
        KardexEntity previous = new KardexEntity();
        previous.NumStockAfter = 10;

        assertEquals(0, movement.NumStockBefore);
        assertEquals(0, movement.NumStockAfter);

        movement.applyLastMovement(previous);

        assertEquals(10, movement.NumStockBefore);
        assertEquals(6, movement.NumStockAfter);
    }

    @Test
    void kardexNegativeStockIsValidatedWhenPreviousMovementIsApplied() {
        KardexEntity movement = KardexEntity.build(
                "V001", 1, "sale_head", "R",
                "P001", "0000", "T001", "A001",
                4, null, null, 1, "ADMIN"
        );
        KardexEntity previous = new KardexEntity();
        previous.NumStockAfter = 3;

        assertThrows(KardexExcepcion.class, () -> movement.applyLastMovement(previous));
    }

    @Test
    void presaleBuilderCreatesPhysicalAndReservedIntentsWithoutBalances() {
        PresaleHeadEntity head = new PresaleHeadEntity();
        head.PresaleCod = "P001";
        head.StoreCod = "T001";
        PresaleDetWarehouseEntity detail = new PresaleDetWarehouseEntity();
        detail.ItemNumber = 1;
        detail.ProductCod = "PR001";
        detail.Variant = "0000";
        detail.WarehouseCod = "A001";
        detail.NumUnit = 4;

        List<KardexZoneEntity> movementList = KardexZoneEntity.buildPresaleReservation(
                head, detail, "ADMIN"
        );

        assertEquals(2, movementList.size());
        assertEquals(KardexZoneConstants.ZONE_PHYSICAL, movementList.get(0).ZoneStockMoved);
        assertEquals(KardexZoneConstants.TYPE_OPERATION_SUBTRACT, movementList.get(0).TypeOperation);
        assertEquals(KardexZoneConstants.ZONE_RESERVED, movementList.get(1).ZoneStockMoved);
        assertEquals(KardexZoneConstants.TYPE_OPERATION_ADD, movementList.get(1).TypeOperation);
        assertEquals(0, movementList.get(0).NumZoneStockBefore);
        assertEquals(0, movementList.get(0).NumZoneStockAfter);
    }
}
