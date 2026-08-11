package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.ProductDeliveryDetailDto;
import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.product.model.entity.ProductPictureEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.service.ProductFindSearchService;
import com.ccadmin.app.product.service.ProductSearchService;
import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDeliverySearchServiceTest {

    @Mock
    private ProductFindSearchService productFindSearchService;
    @Mock
    private ProductSearchService productSearchService;
    @Mock
    private StoreDeliverySearchService storeDeliverySearchService;
    @InjectMocks
    private ProductDeliverySearchService productDeliverySearchService;

    @Test
    void validatesVirtualStoreAndForcesPublicSearchRules() {
        ProductSearchDto request = new ProductSearchDto();
        request.StoreCod = "T001";
        request.StockMin = 0;
        request.SortedBy = "unsafe-column";
        request.DirectionSortedBy = "sideways";
        @SuppressWarnings("unchecked")
        ResponsePageSearchT<ProductSearchEntity> expected = mock(ResponsePageSearchT.class);

        when(storeDeliverySearchService.validateVirtualStore("T001"))
                .thenReturn(new StoreVirtualConfigEntity());
        when(productFindSearchService.query(request)).thenReturn(expected);

        ResponsePageSearchT<ProductSearchEntity> result = productDeliverySearchService.query(request);

        assertEquals(expected, result);
        assertEquals(1, request.StockMin);
        assertEquals("trend", request.SortedBy);
        assertEquals("desc", request.DirectionSortedBy);
        verify(storeDeliverySearchService).validateVirtualStore("T001");
    }

    @Test
    void findsAvailableProductDetailWithItsPictures() {
        ProductSearchEntity product = new ProductSearchEntity();
        product.ProductCod = "TEC007";
        List<ProductPictureEntity> pictureList = List.of(new ProductPictureEntity());

        when(storeDeliverySearchService.validateVirtualStore("T001"))
                .thenReturn(new StoreVirtualConfigEntity());
        when(productFindSearchService.findAvailability("TEC007", "T001")).thenReturn(product);
        when(productSearchService.findPictureList("TEC007")).thenReturn(pictureList);

        ProductDeliveryDetailDto result = productDeliverySearchService.findDetail("TEC007", "T001");

        assertSame(product, result.Product);
        assertSame(pictureList, result.PictureList);
        verify(storeDeliverySearchService).validateVirtualStore("T001");
        verify(productSearchService).findPictureList("TEC007");
    }
}
