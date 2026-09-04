package com.ccadmin.app.sunat.model.dto;

import com.ccadmin.app.sunat.model.idto.ISunatSubmissionSearchDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SunatSubmissionResultDtoTest {

    @Test
    void serializesTheFrontendContractWithPascalCaseFields() throws Exception {
        ISunatSubmissionSearchDto projection = mock(ISunatSubmissionSearchDto.class);
        when(projection.getSunatSubmissionCod()).thenReturn("ES000000000000000001");
        when(projection.getStoreCod()).thenReturn("L001");
        when(projection.getStoreName()).thenReturn("Local principal");
        when(projection.getAttemptCount()).thenReturn(1);

        SunatSubmissionResultDto result = new SunatSubmissionResultDto(projection);
        JsonNode json = new ObjectMapper().readTree(
                new ObjectMapper().writeValueAsString(result)
        );

        assertEquals("ES000000000000000001", json.get("SunatSubmissionCod").asText());
        assertEquals("L001", json.get("StoreCod").asText());
        assertEquals("Local principal", json.get("StoreName").asText());
        assertEquals(1, json.get("AttemptCount").asInt());
        assertFalse(json.has("sunatSubmissionCod"));
        assertFalse(json.has("storeCod"));
    }
}
