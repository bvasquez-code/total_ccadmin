package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.constants.ProductRankingConstants;
import com.ccadmin.app.product.model.entity.ProductRankingEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProductSearchRankingService implements IGenericTaskService {

    private ProductRankingService productRankingService;
    private ResponsePageSearchT<ProductSearchEntity> pageSearch;

    public ProductSearchRankingService(ProductRankingService productRankingService, ResponsePageSearchT<ProductSearchEntity> pageSearch){
        this.pageSearch = pageSearch;
        this.productRankingService = productRankingService;
    }

    @Override
    public void execute() {

        if(this.pageSearch.TotalResult != 1) return;

        List<ProductSearchEntity> productList = this.pageSearch.resultSearch;

        List<ProductRankingEntity> productRankingList = productList
                .stream()
                .map( e-> new ProductRankingEntity(e.ProductCod,e.StoreCod, ProductRankingConstants.PRODUCT_SEARCH).session("SISTEMA"))
                .toList();

        this.productRankingService.saveAll(productRankingList);
    }
}
