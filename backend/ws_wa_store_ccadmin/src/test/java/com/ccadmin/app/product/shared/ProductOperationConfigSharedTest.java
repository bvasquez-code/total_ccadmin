package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductOperationConfigSharedTest {

    @Mock
    private ProductConfigRepository productConfigRepository;
    @Mock
    private ProductInfoRepository productInfoRepository;
    @InjectMocks
    private ProductOperationConfigShared productOperationConfigShared;

    @Test
    void rejectsPhysicalToDigitalConversionWhenProductInfoHasStock() {
        ProductConfigEntity currentConfig = config("N");
        ProductInfoEntity stock = stock();
        stock.NumPhysicalStock = 4;
        stock.NumTotalStock = 4;

        when(productConfigRepository.findForUpdate("P001", "T001")).thenReturn(currentConfig);
        when(productInfoRepository.findInfoStoreForUpdate("P001", "T001")).thenReturn(List.of(stock));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productOperationConfigShared.validateDigitalConversion("P001", "T001", "S")
        );

        assertEquals(
                "El producto P001 no puede convertirse en digital en el local T001"
                        + " porque mantiene stock en la variante 0000"
                        + " (disponible: 0, fisico: 4, no disponible: 0, reservado: 0, total: 4)",
                exception.getMessage()
        );
    }

    @Test
    void allowsPhysicalToDigitalConversionWhenAllProductInfoStockIsZero() {
        when(productConfigRepository.findForUpdate("P001", "T001")).thenReturn(config("N"));
        when(productInfoRepository.findInfoStoreForUpdate("P001", "T001"))
                .thenReturn(List.of(stock()));

        assertDoesNotThrow(
                () -> productOperationConfigShared.validateDigitalConversion("P001", "T001", "S")
        );
    }

    @Test
    void doesNotReadStockWhenTargetConfigurationIsPhysical() {
        assertDoesNotThrow(
                () -> productOperationConfigShared.validateDigitalConversion("P001", "T001", "N")
        );

        verify(productConfigRepository, never()).findForUpdate("P001", "T001");
        verify(productInfoRepository, never()).findInfoStoreForUpdate("P001", "T001");
    }

    private ProductConfigEntity config(String isDigital) {
        ProductConfigEntity config = new ProductConfigEntity();
        config.ProductCod = "P001";
        config.StoreCod = "T001";
        config.IsDigital = isDigital;
        return config;
    }

    private ProductInfoEntity stock() {
        ProductInfoEntity stock = new ProductInfoEntity();
        stock.ProductCod = "P001";
        stock.Variant = "0000";
        stock.StoreCod = "T001";
        return stock;
    }
}
