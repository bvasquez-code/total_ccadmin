package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.ProductRankingConstants;
import com.ccadmin.app.product.model.entity.ProductRankingEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.shared.service.IGenericTaskService;

import java.util.List;

public class PresaleRankingService implements IGenericTaskService {

    private ProductRankingService productRankingService;

    private PresaleDetailDto presaleDetail;

    public PresaleRankingService(ProductRankingService productRankingService,PresaleDetailDto presaleDetail){
        this.presaleDetail = presaleDetail;
        this.productRankingService = productRankingService;
    }

    @Override
    public void execute() {
        List<ProductRankingEntity> productRankingList = this.presaleDetail.DetailList
                .stream()
                .map( product -> new ProductRankingEntity(
                             product.ProductCod
                            ,this.presaleDetail.Headboard.StoreCod
                            , ProductRankingConstants.PRODUCT_ADD_CAR
                        ).session("SISTEMA")
                )
                .toList();

        this.productRankingService.saveAll(productRankingList);
    }
}
