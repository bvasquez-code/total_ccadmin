package com.ccadmin.app.inventory.service;

import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementValidationServiceTest {

    @Mock
    private BusinessConfigRepository businessConfigRepository;
    @Mock
    private ProductOperationConfigShared productOperationConfigShared;
    @InjectMocks
    private StockMovementValidationService stockMovementValidationService;

    @Test
    void rejectsDigitalProductInStockMovement() {
        when(productOperationConfigShared.isDigital("DIG-01", "ST01"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementValidationService.requirePhysicalProducts(
                        List.of("DIG-01"), "ST01"
                )
        );

        assertEquals(
                "El producto DIG-01 es digital y no puede utilizarse en movimientos de stock",
                exception.getMessage()
        );
    }

    @Test
    void acceptsPhysicalProductsAndChecksDuplicatesOnce() {
        when(productOperationConfigShared.isDigital("PHY-01", "ST01"))
                .thenReturn(false);

        assertDoesNotThrow(
                () -> stockMovementValidationService.requirePhysicalProducts(
                        List.of("PHY-01", "PHY-01"), "ST01"
                )
        );

        verify(productOperationConfigShared, times(1))
                .isDigital("PHY-01", "ST01");
    }
}
