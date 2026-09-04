package com.ccadmin.app.inventory.model.factory;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
import com.ccadmin.app.inventory.model.dto.StockEntryRegisterDto;
import com.ccadmin.app.inventory.model.dto.StockExitRegisterDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;
import com.ccadmin.app.inventory.model.entity.StockExitDetEntity;
import com.ccadmin.app.inventory.model.entity.StockExitHeadEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class InventoryModelFactoryTest {

    @Test
    void createsBulkStockEntryHead() {
        StockEntryBulkCreateDto request = bulkRequest();

        StockEntryHeadEntity result =
                StockEntryHeadEntityFactory.fromBulkCreate(request);

        assertEquals(request.StockEntryCod, result.StockEntryCod);
        assertEquals(request.StoreCod, result.StoreCod);
        assertEquals(StockMovementConstants.PROCESS_ORIGINAL, result.ProcessType);
        assertEquals(StockMovementConstants.MODE_DIRECT, result.MovementMode);
        assertEquals(BulkLoadConstants.STOCK_REASON, result.ReasonCode);
        assertNull(result.OriginStockEntryCod);
        assertEquals(StatusConst.PENDING, result.ProcessStatus);
        assertEquals(
                "Generado por carga masiva " + request.BulkLoadCod,
                result.Observation
        );
    }

    @Test
    void createsBulkStockEntryDetailsInSourceOrder() {
        StockEntryBulkCreateDto request = bulkRequest();

        List<StockEntryDetEntity> result =
                StockEntryDetEntityFactory.fromBulkCreate(request);

        assertEquals(1, result.size());
        StockEntryDetEntity detail = result.getFirst();
        StockEntryBulkLineDto source = request.DetailList.getFirst();
        assertEquals(source.ProductCod, detail.ProductCod);
        assertEquals(source.Variant, detail.Variant);
        assertEquals(source.WarehouseCod, detail.WarehouseCod);
        assertEquals(source.LotNumber, detail.LotNumber);
        assertEquals(source.ExpirationDate, detail.ExpirationDate);
        assertEquals(source.ProductUnitName, detail.ProductUnitName);
        assertEquals(source.ProductUnitFactor, detail.ProductUnitFactor);
        assertEquals(source.NumUnit, detail.NumUnit);
        assertEquals(source.NumUnitPrice, detail.NumUnitPrice);
        assertEquals(
                "Carga masiva " + request.BulkLoadCod
                        + ", fila Excel " + source.SourceRowNumber,
                detail.Observation
        );
    }

    @Test
    void createsBulkResultWithPersistedItemNumbers() {
        StockEntryBulkCreateDto request = bulkRequest();
        StockEntryHeadEntity head =
                StockEntryHeadEntityFactory.fromBulkCreate(request);
        StockEntryDetEntity detail =
                StockEntryDetEntityFactory.fromBulkCreate(request).getFirst();
        detail.ItemNumber = 3;

        StockEntryBulkResultDto result =
                StockEntryBulkResultDtoFactory.fromConfirmedBulk(
                        head, request.DetailList, List.of(detail)
                );

        assertEquals(request.StockEntryCod, result.StockEntryCod);
        assertEquals(3, result.ItemNumberByReference.get(25));
    }

    @Test
    void assemblesStockEntryRegisterDto() {
        StockEntryHeadEntity head = new StockEntryHeadEntity();
        List<StockEntryDetEntity> details = List.of(new StockEntryDetEntity());

        StockEntryRegisterDto result =
                StockEntryRegisterDtoFactory.fromEntities(head, details);

        assertSame(head, result.Head);
        assertSame(details, result.DetailList);
    }

    @Test
    void assemblesStockExitRegisterDto() {
        StockExitHeadEntity head = new StockExitHeadEntity();
        List<StockExitDetEntity> details = List.of(new StockExitDetEntity());

        StockExitRegisterDto result =
                StockExitRegisterDtoFactory.fromEntities(head, details);

        assertSame(head, result.Head);
        assertSame(details, result.DetailList);
    }

    private StockEntryBulkCreateDto bulkRequest() {
        StockEntryBulkCreateDto request = new StockEntryBulkCreateDto();
        request.StockEntryCod = "IET0010001000001";
        request.StoreCod = "T001";
        request.BulkLoadCod = "CM00000000000001";

        StockEntryBulkLineDto line = new StockEntryBulkLineDto();
        line.ReferenceItemNumber = 25;
        line.SourceRowNumber = 4;
        line.ProductCod = "TEC008";
        line.Variant = "0000";
        line.WarehouseCod = "T0010001";
        line.ProductUnitName = "NIU";
        line.ProductUnitFactor = 1;
        line.NumUnit = 7;
        line.NumUnitPrice = new BigDecimal("12.50");
        line.LotNumber = "LOTE-001";
        line.ExpirationDate = Date.valueOf("2027-10-30");
        request.DetailList.add(line);
        return request;
    }
}
