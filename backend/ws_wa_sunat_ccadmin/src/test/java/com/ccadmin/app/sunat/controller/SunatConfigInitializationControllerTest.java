package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.sunat.model.dto.SunatInitializationFormDto;
import com.ccadmin.app.sunat.model.dto.SunatInitializationRequestDto;
import com.ccadmin.app.sunat.service.SunatConfigCreateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SunatConfigInitializationControllerTest {
    @Test
    void acceptsBrowserMultipartFieldsAndPfxFile() throws Exception {
        SunatConfigCreateService service = mock(SunatConfigCreateService.class);
        when(service.configureInitialization(any(), any())).thenReturn(new SunatInitializationFormDto());
        SunatConfigController controller = new SunatConfigController();
        ReflectionTestUtils.setField(controller, "sunatConfigCreateService", service);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var configuration = new MockMultipartFile("configuration", "", "text/plain",
                "{\"Mode\":\"PRODUCCION\",\"SolUser\":\"SOLUSER\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var certificate = new MockMultipartFile("certificate", "company.pfx", "application/octet-stream", new byte[]{1, 2});
        mvc.perform(multipart("/api/v1/sunat/config/initialization").file(configuration).file(certificate))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ErrorStatus").value(false));
        ArgumentCaptor<SunatInitializationRequestDto> captured = ArgumentCaptor.forClass(SunatInitializationRequestDto.class);
        verify(service).configureInitialization(captured.capture(), any());
        assertEquals("PRODUCCION", captured.getValue().Mode);
        assertEquals("SOLUSER", captured.getValue().SolUser);
    }
}
