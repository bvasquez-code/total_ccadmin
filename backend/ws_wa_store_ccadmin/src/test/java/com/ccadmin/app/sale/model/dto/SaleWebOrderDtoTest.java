package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.idto.ISaleWebOrderDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaleWebOrderDtoTest {

    @Test
    void serializesUsingTheAdministrationFrontendContract() throws Exception {
        ISaleWebOrderDto projection = mock(ISaleWebOrderDto.class);
        when(projection.getSaleCod()).thenReturn("ST001");
        when(projection.getDeliveryStatus()).thenReturn("R");

        String json = new ObjectMapper().writeValueAsString(SaleWebOrderDto.from(projection));

        assertTrue(json.contains("\"SaleCod\":\"ST001\""));
        assertTrue(json.contains("\"DeliveryStatus\":\"R\""));
        assertFalse(json.contains("\"saleCod\""));
    }
}
