package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductInfoStockDto;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductInfoStockSearchService {

    private static final int PAGE_SIZE = 10;

    private final ProductInfoRepository productInfoRepository;
    private final ProductShared productShared;

    public ProductInfoStockSearchService(
            ProductInfoRepository productInfoRepository,
            ProductShared productShared
    ) {
        this.productInfoRepository = productInfoRepository;
        this.productShared = productShared;
    }

    public ResponsePageSearchT<ProductInfoStockDto> findAll(SearchDto search) {
        this.normalize(search);

        SearchTService<ProductInfoEntity> searchService = new SearchTService<>(this.productInfoRepository);
        ResponsePageSearchT<ProductInfoEntity> productInfoPage = searchService.findAllStore(search, PAGE_SIZE);
        List<ProductInfoEntity> productInfoList = productInfoPage.resultSearch;
        Map<String, ProductEntity> productByCode = this.findProducts(productInfoList);
        List<ProductInfoStockDto> result = productInfoList.stream()
                .map(productInfo -> new ProductInfoStockDto(
                        productInfo,
                        productByCode.get(productInfo.ProductCod)
                ))
                .toList();

        ResponsePageSearchT<ProductInfoStockDto> response = new ResponsePageSearchT<>();
        response.clone(result, productInfoPage);
        response.StarResult = response.TotalResult == 0 ? 0 : response.StarResult;
        response.EndResult = Math.min(response.EndResult, response.TotalResult);
        return response;
    }

    private Map<String, ProductEntity> findProducts(List<ProductInfoEntity> productInfoList) {
        List<String> productCodList = productInfoList.stream()
                .map(productInfo -> productInfo.ProductCod)
                .distinct()
                .toList();

        return productCodList.isEmpty()
                ? Map.of()
                : this.productShared.findAllById(productCodList).stream()
                        .collect(Collectors.toMap(product -> product.ProductCod, Function.identity()));
    }

    private void normalize(SearchDto search) {
        if (search == null) {
            throw new IllegalArgumentException("Los filtros de stock por zona son obligatorios");
        }
        search.Query = search.Query == null ? "" : search.Query.trim();
        search.StoreCod = search.StoreCod == null ? "" : search.StoreCod.trim();
        search.Page = Math.max(search.Page, 1);

    }
}
