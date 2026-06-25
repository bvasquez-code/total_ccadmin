package com.ccadmin.app.promotion.service;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.promotion.model.constant.PromotionConstant;
import com.ccadmin.app.promotion.model.dto.PromotionBrandRegisterDto;
import com.ccadmin.app.promotion.model.dto.PromotionCategoryRegisterDto;
import com.ccadmin.app.promotion.model.dto.PromotionProductRegisterDto;
import com.ccadmin.app.promotion.model.entity.PromotionEntity;
import com.ccadmin.app.promotion.model.entity.PromotionProductEntity;
import com.ccadmin.app.promotion.model.entity.PromotionStoreEntity;
import com.ccadmin.app.promotion.repository.PromotionProductRepository;
import com.ccadmin.app.promotion.repository.PromotionRepository;
import com.ccadmin.app.promotion.repository.PromotionStoreRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionCreateService extends SessionService {
    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private PromotionStoreRepository promotionStoreRepository;
    @Autowired
    private PromotionProductRepository promotionProductRepository;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private ProductShared productShared;

    public String generateCod(){
        return this.promotionRepository.getCodSeq("promotion");
    }

    @Transactional
    public PromotionEntity save(PromotionProductRegisterDto promoProduct){
        PromotionEntity promotion = this.promotionRepository.save(promoProduct.Promotion);
        promoProduct.ProductList.forEach( e -> e.addSession(getUserCod()) );
        this.promotionProductRepository.saveAll(promoProduct.ProductList);

        if( promoProduct.StoreList == null || promoProduct.StoreList.size() == 0 ){
            List<StoreEntity> StoreAvailable = this.storeShared.findAll();
            promoProduct.StoreList = StoreAvailable.stream()
                    .map( e -> new PromotionStoreEntity(promoProduct.Promotion.PromotionCod,e.StoreCod))
                    .collect(Collectors.toList());
        }
        this.promotionStoreRepository.saveAll(promoProduct.StoreList);
        return promotion;
    }

    @Transactional
    public PromotionEntity save(PromotionBrandRegisterDto promoBrand){

        PromotionProductRegisterDto promoProductRegister = new PromotionProductRegisterDto();
        promoProductRegister.Promotion = promoBrand.Promotion;
        promoProductRegister.StoreList = promoBrand.StoreList;

        for(var Brand : promoBrand.BrandList){
            List<ProductEntity> productList = this.productShared.findByBrandCod(Brand.BrandCod);
            List<PromotionProductEntity> ProductPromoList = productList.stream()
                    .map( Product -> new PromotionProductEntity()
                            .buildSimple(
                                 promoBrand.Promotion.PromotionCod
                                ,Product.ProductCod
                                ,Brand.TypeDiscount
                                ,Brand.DiscountAmount
                            )
                    ).collect(Collectors.toList());
            promoProductRegister.ProductList.addAll(ProductPromoList);
        }

        return this.save(promoProductRegister);
    }

    @Transactional
    public PromotionEntity save(PromotionCategoryRegisterDto promoCategory){

        PromotionProductRegisterDto promoProductRegister = new PromotionProductRegisterDto();
        promoProductRegister.Promotion = promoCategory.Promotion;
        promoProductRegister.StoreList = promoCategory.StoreList;

        for(var Category : promoCategory.CategoryList){
            List<ProductEntity> productList = this.productShared.findByBrandCod(Category.CategoryCod);
            List<PromotionProductEntity> ProductPromoList = productList.stream()
                    .map( Product -> new PromotionProductEntity()
                            .buildSimple(
                                     promoCategory.Promotion.PromotionCod
                                    ,Product.ProductCod
                                    ,Category.TypeDiscount
                                    ,Category.DiscountAmount
                            )
                    )
                    .collect(Collectors.toList());
            promoProductRegister.ProductList.addAll(ProductPromoList);
        }
        return this.save(promoProductRegister);
    }

    private PromotionProductRegisterDto reorganizeProduct(PromotionProductRegisterDto promoProduct){

        List<PromotionProductEntity> ProductList = new ArrayList<>();
        int position = 0;

        if(promoProduct.Promotion.WayOfUse.equals(PromotionConstant.WayOfUse.DOS_X_UNO)){

            for(var ProductPromotion : promoProduct.ProductList){
                position++;
                int Package = position;
                ProductPromotion.setPackage(Package,1).setDiscount(new BigDecimal(0));
                ProductList.add(ProductPromotion);
                List<PromotionProductEntity> ProductListDiff = promoProduct.ProductList
                        .stream()
                        .filter( e -> !e.ProductCod.equals(ProductPromotion.ProductCod))
                        .collect(Collectors.toList());
                ProductListDiff.forEach(e-> e.setPackage(Package,2).setDiscount(new BigDecimal(100)) );
                ProductList.addAll(ProductListDiff);
            }
            promoProduct.ProductList = ProductList;
        }

        return promoProduct;
    }
}
