package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.service.PresaleDeliveryCreateService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresaleDeliveryControllerTest {

    @Mock
    private PresaleDeliveryCreateService presaleDeliveryCreateService;

    @Test
    void returnsGeneratedCodeInDataInsteadOfTreatingItAsAMessage() {
        when(presaleDeliveryCreateService.createCode("T001"))
                .thenReturn("PT00100010000300");
        PresaleDeliveryController controller = new PresaleDeliveryController(
                presaleDeliveryCreateService
        );

        ResponseEntity<ResponseWsDto> response = controller.createCode("T001");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PT00100010000300", response.getBody().Data);
        assertFalse(response.getBody().ErrorStatus);
    }
}
