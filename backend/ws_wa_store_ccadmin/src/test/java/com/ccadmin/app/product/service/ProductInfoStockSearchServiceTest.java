package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductInfoStockDto;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductInfoStockSearchServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;
    @Mock
    private ProductShared productShared;

    @Test
    void findAllReturnsProductInfoWithProductDescription() {
        ProductInfoStockSearchService service = new ProductInfoStockSearchService(
                productInfoRepository,
                productShared
        );
        SearchDto search = new SearchDto(" laptop ", 1, "ST01");
        ProductInfoEntity productInfo = new ProductInfoEntity();
        productInfo.ProductCod = "P001";
        productInfo.Variant = "0000";
        productInfo.StoreCod = "ST01";
        productInfo.NumPhysicalStock = 8;
        productInfo.NumReservedStock = 2;
        productInfo.NumUnavailableStock = 1;
        productInfo.NumTotalStock = 11;
        ProductEntity product = new ProductEntity();
        product.ProductCod = "P001";
        product.ProductName = "Laptop";

        when(productInfoRepository.findByQueryTextStore("laptop", "laptop", "ST01", 0, 10))
                .thenReturn(List.of(productInfo));
        when(productInfoRepository.countByQueryTextStore("laptop", "laptop", "ST01")).thenReturn(1);
        when(productShared.findAllById(List.of("P001"))).thenReturn(List.of(product));

        ResponsePageSearchT<ProductInfoStockDto> response = service.findAll(search);
        List<ProductInfoStockDto> result = response.resultSearch;

        assertEquals(1, response.TotalResult);
        assertEquals(1, response.StarResult);
        assertEquals(1, response.EndResult);
        assertSame(productInfo, result.get(0).productInfo);
        assertSame(product, result.get(0).product);
        verify(productInfoRepository).findByQueryTextStore("laptop", "laptop", "ST01", 0, 10);
    }

    @Test
    void findAllAllowsQueryingAllStores() {
        ProductInfoStockSearchService service = new ProductInfoStockSearchService(
                productInfoRepository,
                productShared
        );
        SearchDto search = new SearchDto("", 1, "");
        when(productInfoRepository.findByQueryTextStore("", "", "", 0, 10)).thenReturn(List.of());
        when(productInfoRepository.countByQueryTextStore("", "", "")).thenReturn(0);

        ResponsePageSearchT<ProductInfoStockDto> response = service.findAll(search);

        assertEquals(0, response.TotalResult);
        assertEquals(0, response.StarResult);
        assertEquals(0, response.EndResult);
        verify(productInfoRepository).findByQueryTextStore("", "", "", 0, 10);
    }
}
