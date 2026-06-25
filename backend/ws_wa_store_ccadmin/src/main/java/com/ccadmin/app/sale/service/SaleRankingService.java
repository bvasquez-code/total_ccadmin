package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.ProductRankingConstants;
import com.ccadmin.app.product.model.entity.ProductRankingEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.shared.service.IGenericTaskService;

import java.util.List;

public class SaleRankingService implements IGenericTaskService {

    private SaleDetailDto saleDetail;
    private ProductRankingService productRankingService;
    public SaleRankingService(
            ProductRankingService productRankingService
            ,SaleDetailDto saleDetail
    ){
        this.saleDetail = saleDetail;
        this.productRankingService = productRankingService;
    }
    @Override
    public void execute() {

        List<ProductRankingEntity> productRankingList = this.saleDetail.DetailList
                .stream()
                .map( product -> new ProductRankingEntity(
                             product.ProductCod
                            ,this.saleDetail.Headboard.StoreCod
                            ,ProductRankingConstants.PRODUCT_SALE
                        ).session("SISTEMA")
                )
                .toList();

        this.productRankingService.saveAll(productRankingList);
    }
}
