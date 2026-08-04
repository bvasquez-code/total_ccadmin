package com.ccadmin.app.sunat.identity.controller;

import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityDto;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.service.SunatIdentitySearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SunatIdentityControllerTest {

    private SunatIdentitySearchService sunatIdentitySearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.sunatIdentitySearchService = mock(SunatIdentitySearchService.class);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new SunatIdentityController(this.sunatIdentitySearchService))
                .build();
    }

    @Test
    void returnsCompanyFieldsInEnglishInsideStandardResponse() throws Exception {
        CompanyIdentityDto company = new CompanyIdentityDto(
                "20123456789",
                "EMPRESA EJEMPLO S.A.C.",
                "SOCIEDAD ANONIMA CERRADA",
                "EMPRESA EJEMPLO",
                null,
                null,
                "ACTIVO",
                "HABIDO",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                null
        );
        when(this.sunatIdentitySearchService.findCompanyByRuc("20123456789"))
                .thenReturn(new CompanyIdentityResponseDto(true, "Consulta realizada correctamente.", company));

        this.mockMvc.perform(get("/api/v1/sunatIdentity/findCompanyByRuc")
                        .param("Ruc", "20123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Status").value("200"))
                .andExpect(jsonPath("$.Data.found").value(true))
                .andExpect(jsonPath("$.Data.company.legalName").value("EMPRESA EJEMPLO S.A.C."))
                .andExpect(jsonPath("$.Data.company.taxpayerStatus").value("ACTIVO"));
    }

    @Test
    void keepsProjectErrorContractForInvalidInput() throws Exception {
        when(this.sunatIdentitySearchService.findCompanyByRuc("123"))
                .thenThrow(new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos."));

        this.mockMvc.perform(get("/api/v1/sunatIdentity/findCompanyByRuc").param("Ruc", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.Status").value("500"))
                .andExpect(jsonPath("$.ErrorStatus").value(true))
                .andExpect(jsonPath("$.Message").value("El RUC debe contener exactamente 11 dígitos."));
    }
}
