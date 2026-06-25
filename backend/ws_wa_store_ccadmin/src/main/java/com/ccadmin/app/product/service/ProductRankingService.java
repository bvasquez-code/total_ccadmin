package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.ProductRankingEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.model.entity.id.ProductRankingID;
import com.ccadmin.app.product.model.entity.id.ProductSearchID;
import com.ccadmin.app.product.repository.ProductRankingRepository;
import com.ccadmin.app.product.repository.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductRankingService {

    @Autowired
    private ProductRankingRepository productRankingRepository;
    @Autowired
    private ProductSearchRepository productSearchRepository;

    public void saveAll(List<ProductRankingEntity> productRankingList){

        List<ProductRankingEntity> productRankingListDB = this.productRankingRepository.findAllById(
          productRankingList.stream()
                  .map(e -> new ProductRankingID(e.ProductCod,e.StoreCod))
                  .toList()
        ).stream().map(e -> {

            ProductRankingEntity productRanking = productRankingList.stream()
                    .filter( p -> p.ProductCod.equals(e.ProductCod) && p.StoreCod.equals(e.StoreCod))
                    .findFirst()
                    .orElse(new ProductRankingEntity());

            e.plusRankingPoints(productRanking.RankingPoints);
            return e;
        }
        ).toList();

        List<String> productCodList = productRankingListDB.stream().map( e -> e.ProductCod).toList();

        List<ProductRankingEntity> productRankingListNew = productRankingList
                .stream()
                .filter( e -> !productCodList.contains(e.ProductCod) )
                .toList();

        List<ProductRankingEntity> productRankingListAll = new ArrayList<>();

        if(!productRankingListDB.isEmpty()) productRankingListAll.addAll(productRankingListDB);
        if(!productRankingListNew.isEmpty()) productRankingListAll.addAll(productRankingListNew);

        List<ProductSearchEntity> productSearchList = this.productSearchRepository.findAllById(
                        productRankingListDB.stream()
                        .map( e -> new ProductSearchID(e.ProductCod,e.StoreCod))
                        .toList()
                )
                .stream()
                .map( e -> this.updateNumTrend(e,productRankingListDB))
                .toList();

        this.productRankingRepository.saveAll(productRankingListAll);
        this.productSearchRepository.saveAll(productSearchList);
    }

    private ProductSearchEntity updateNumTrend(ProductSearchEntity productSearch,List<ProductRankingEntity> productRankingList){
        ProductRankingEntity productRanking = productRankingList
                .stream()
                .filter(e -> e.ProductCod.equals(productSearch.ProductCod) && e.StoreCod.equals(productSearch.StoreCod))
                .findFirst().orElse(new ProductRankingEntity());
        productSearch.NumTrend = productRanking.RankingPoints;
        return productSearch;
    }
}
