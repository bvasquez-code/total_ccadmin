package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.*;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.model.entity.id.ProductRankingID;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.AppFileShared;
import com.ccadmin.app.system.shared.CurrencyShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProductFindCreateService extends SessionService {

    @Autowired
    private ProductSearchRepository productSearchRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductRankingRepository productRankingRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductPictureRepository productPictureRepository;
    @Autowired
    private CurrencyShared currencyShared;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private AppFileShared appFileShared;

    @Transactional
    public ProductSearchEntity save(String ProductCod, String StoreCod)
    {
        log.info("GENERAR INFO PRODUCTO : {} PARA LA TIENDA {}",ProductCod,StoreCod);
        ProductEntity product = this.productRepository.findById(ProductCod).get();
        ProductConfigEntity productConfig = this.productConfigRepository.findById(new ProductConfigID(ProductCod, StoreCod))
                .orElseGet(() -> this.productConfigRepository.findAnyByProductCod(ProductCod));
        List<ProductVariantEntity> variantList = this.productVariantRepository.findAllVariantProduct(ProductCod);
        List<ProductInfoEntity> productInfoList = this.productInfoRepository.findAllById(
                variantList.stream().map( variant -> new ProductInfoId(variant.ProductCod,variant.Variant,StoreCod)).toList()
        );
        CategoryEntity category = this.categoryRepository.findById(product.CategoryCod).get();
        CategoryEntity categoryDad = this.categoryRepository.findById(category.CategoryDadCod).get();
        BrandEntity brand = this.brandRepository.findById(product.BrandCod).get();
        CurrencyEntity currencySys = this.currencyShared.findCurrencySystem();
        ProductPictureEntity productPicture = this.productPictureRepository.findPrincipal(ProductCod);
        AppFileEntity appFile = null;
        ProductRankingEntity productRanking = new ProductRankingEntity();
        Optional<ProductRankingEntity> productRankingOp = this.productRankingRepository.findById(new ProductRankingID(ProductCod,StoreCod));

        if(productRankingOp.isPresent()){
            productRanking = productRankingOp.get();
        }

        if( productPicture!=null )
        {
            appFile = this.appFileShared.findById(productPicture.FileCod);
        }

        ProductSearchEntity productSearch = new ProductSearchEntity(
                 product
                ,productConfig
                ,productInfoList
                ,category
                ,categoryDad
                ,brand
                ,currencySys
                ,productPicture
                ,appFile
                ,productRanking
                ,StoreCod
        );
        productSearch.addSession(getUserCod());

        log.info("PRODUCTO RESUMEN : {} => {}",ProductCod,productSearch.toString());
        return this.productSearchRepository.save(productSearch);
    }

    public List<ProductSearchEntity> generateSearch(String ProductCod)
    {
        List<ProductSearchEntity> productSearchList = new ArrayList<>();

        List<StoreEntity> storeList = this.storeShared.findAll();

        for (var store : storeList)
        {
            save(ProductCod,store.StoreCod);
        }

        return productSearchList;
    }
}
