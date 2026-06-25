package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.product.model.entity.ProductBarcodeEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.repository.ProductBarcodeRepository;
import com.ccadmin.app.product.repository.ProductSearchRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.GenericQueuedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ProductFindSearchService {

    @Autowired
    private ProductSearchRepository productSearchRepository;
    @Autowired
    private ProductBarcodeRepository productBarcodeRepository;
    @Autowired
    private ProductRankingService productRankingService;
    @Autowired
    private GenericQueuedService genericQueuedService;

    public ResponsePageSearchT<ProductSearchEntity> query(ProductSearchDto productSearch)
    {
        if(productSearch.Query != null && isPositiveInteger(productSearch.Query)){
            ProductBarcodeEntity productBarcode = this.productBarcodeRepository.findById(productSearch.Query).orElse(null);
            if(productBarcode!=null){
                productSearch.Query = productBarcode.ProductCod;
            }
        }

        int countResult = this.productSearchRepository.countByQueryTextStorePersonalized(
                productSearch.Query,
                productSearch.Query,
                productSearch.StoreCod,
                productSearch.StockMin
        );
        int limitSearchProduct = 12;
        productSearch.setLimit(limitSearchProduct);

        if(productSearch.SortedBy == null || productSearch.SortedBy.isEmpty()){
            productSearch.SortedBy = "trend";
        }
        if(productSearch.DirectionSortedBy == null || productSearch.DirectionSortedBy.isEmpty()){
            productSearch.DirectionSortedBy = "desc";
        }

        List<ProductSearchEntity> productSearchList = ( countResult > 0 ) ? this.productSearchRepository.findByQueryTextStorePersonalized(
                productSearch.Query,
                productSearch.Query,
                productSearch.StoreCod,
                productSearch.StockMin,
                productSearch.SortedBy,
                productSearch.DirectionSortedBy,
                productSearch.Init,
                productSearch.Limit
        ) : new ArrayList<>();

        ResponsePageSearchT<ProductSearchEntity> rpt = new ResponsePageSearchT<>(
                productSearchList,
                productSearch.Page,
                productSearch.Limit,
                countResult
        );
        this.rankingProduct(rpt);
        return rpt;
    }

    private void rankingProduct(ResponsePageSearchT<ProductSearchEntity> search){
        ProductSearchRankingService productSearchRankingService = new ProductSearchRankingService(this.productRankingService,search);
        this.genericQueuedService.addQueued(productSearchRankingService);
    }

    public boolean isPositiveInteger(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("\\d+");
    }
}
