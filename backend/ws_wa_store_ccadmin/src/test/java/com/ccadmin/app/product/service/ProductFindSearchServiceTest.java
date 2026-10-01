package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.repository.ProductBarcodeRepository;
import com.ccadmin.app.product.repository.ProductSearchRepository;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.system.service.AppFilePublicUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFindSearchServiceTest {
    @Mock private ProductSearchRepository productSearchRepository;
    @Mock private ProductBarcodeRepository productBarcodeRepository;
    @Mock private ProductRankingService productRankingService;
    @Mock private GenericQueuedService genericQueuedService;
    @Mock private AppFilePublicUrlService appFilePublicUrlService;
    @InjectMocks private ProductFindSearchService productFindSearchService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"internal", " internal ", " "})
    void keepsAllProductsForInternalAndLegacySearches(String origin) {
        assertSearchFilter(origin, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"external", " external "})
    void filtersPublicProductsInCountAndPageForExternalSearches(String origin) {
        assertSearchFilter(origin, true);
    }

    private void assertSearchFilter(String origin, boolean publicOnly) {
        ProductSearchDto request = new ProductSearchDto();
        request.Query = "producto";
        request.StoreCod = "T001";
        request.Page = 2;
        request.SearchOrigin = origin;
        ProductSearchEntity product = new ProductSearchEntity();
        when(productSearchRepository.countByQueryTextStorePersonalized(
                "producto", "producto", "T001", 0, publicOnly)).thenReturn(13);
        when(productSearchRepository.findByQueryTextStorePersonalized(
                "producto", "producto", "T001", 0, publicOnly, "trend", "desc", 12, 12))
                .thenReturn(List.of(product));

        var result = productFindSearchService.query(request);

        assertEquals(13, result.TotalResult);
        assertEquals(2, result.TotalPages);
        assertSame(product, result.resultSearch.getFirst());
        verify(productSearchRepository).findByQueryTextStorePersonalized(
                "producto", "producto", "T001", 0, publicOnly, "trend", "desc", 12, 12);
    }

    @Test
    void rejectsUnknownSearchOrigin() {
        ProductSearchDto request = new ProductSearchDto();
        request.SearchOrigin = "UNKNOWN";
        assertThrows(IllegalArgumentException.class, () -> productFindSearchService.query(request));
        verifyNoInteractions(productSearchRepository);
    }

    @Test
    void rejectsProductUnavailableToPublicWhileKeepingLegacyAvailability() {
        when(productSearchRepository.findAvailableProduct("P001", "T001", true))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> productFindSearchService.findAvailability("P001", "T001", true));
        ProductSearchEntity product = new ProductSearchEntity();
        when(productSearchRepository.findAvailableProduct("P001", "T001", false))
                .thenReturn(Optional.of(product));
        assertSame(product, productFindSearchService.findAvailability("P001", "T001"));
    }
}
