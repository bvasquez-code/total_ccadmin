package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IMerchandiseProfitabilityDto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchandiseProfitabilityDtoTest {
    @Test
    void commercialResultSubtractsSoldCostAndInventoryLosses() {
        var result = new MerchandiseProfitabilityDto(row("45.00", "37.80", "4.20"));
        assertEquals(new BigDecimal("3.00"), result.EstimatedResult);
        assertEquals("PEN", result.CurrencyCod);
    }

    @Test
    void preservesNegativeAndZeroResults() {
        assertEquals(new BigDecimal("-4.20"),
                new MerchandiseProfitabilityDto(row("0.00", "0.00", "4.20")).EstimatedResult);
        assertEquals(new BigDecimal("0.00"),
                new MerchandiseProfitabilityDto(row("10.00", "10.00", "0.00")).EstimatedResult);
    }

    @Test
    void incompatibleOrMissingCostsDoNotProduceACommercialResult() {
        var row = row("45.00", "37.80", "4.20");
        when(row.getMissingCostRows()).thenReturn(1L);
        var result = new MerchandiseProfitabilityDto(row);
        assertEquals(new BigDecimal("45.00"), result.NetSales);
        assertNull(result.CostOfGoodsSold);
        assertNull(result.StockLosses);
        assertNull(result.EstimatedResult);
    }

    private IMerchandiseProfitabilityDto row(String sales, String cost, String losses) {
        var row = mock(IMerchandiseProfitabilityDto.class);
        when(row.getCurrencyCod()).thenReturn("PEN");
        when(row.getNetSales()).thenReturn(new BigDecimal(sales));
        when(row.getCostOfGoodsSold()).thenReturn(new BigDecimal(cost));
        when(row.getStockLosses()).thenReturn(new BigDecimal(losses));
        return row;
    }
}
