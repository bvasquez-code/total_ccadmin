package com.ccadmin.app.delivery.service;

import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.service.ProductFindSearchService;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ProductDeliverySearchService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("trend", "price", "date");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

    private final ProductFindSearchService productFindSearchService;
    private final StoreDeliverySearchService storeDeliverySearchService;

    public ProductDeliverySearchService(
            ProductFindSearchService productFindSearchService,
            StoreDeliverySearchService storeDeliverySearchService
    ) {
        this.productFindSearchService = productFindSearchService;
        this.storeDeliverySearchService = storeDeliverySearchService;
    }

    public ResponsePageSearchT<ProductSearchEntity> query(ProductSearchDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de búsqueda son obligatorios");
        }
        storeDeliverySearchService.validateVirtualStore(request.StoreCod);

        request.StockMin = 1;
        request.SortedBy = normalize(request.SortedBy, ALLOWED_SORT_FIELDS, "trend");
        request.DirectionSortedBy = normalize(
                request.DirectionSortedBy,
                ALLOWED_SORT_DIRECTIONS,
                "desc"
        );
        return productFindSearchService.query(request);
    }

    public ProductSearchEntity findAvailability(String productCod, String storeCod) {
        storeDeliverySearchService.validateVirtualStore(storeCod);
        return productFindSearchService.findAvailability(productCod, storeCod);
    }

    private String normalize(String value, Set<String> allowedValues, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        return allowedValues.contains(normalized) ? normalized : defaultValue;
    }
}
