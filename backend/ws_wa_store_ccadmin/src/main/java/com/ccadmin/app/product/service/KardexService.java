package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.KardexDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.product.shared.*;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.shared.PucharseHeadShared;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.shared.SaleHeadShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class KardexService extends SessionService {

    @Autowired
    private KardexRepository kardexRepository;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;
    @Autowired
    private ProductInfoShared productInfoShared;
    @Autowired
    private ProductInfoWarehouseShared productInfoWarehouseShared;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private SaleHeadShared saleHeadShared;
    @Autowired
    private PucharseHeadShared pucharseHeadShared;

    @Autowired
    private BusinessConfigSearchService businessConfigSearchService;
    private SearchService searchService;

    @Transactional
    public KardexEntity save(KardexEntity kardex) {
        kardex = this.kardexRepository.save(kardex);
        this.saveInfoProduct(kardex);
        return kardex;
    }

    @Transactional
    public List<KardexEntity> saveAll(List<KardexEntity> kardexList) {
        kardexList.forEach(KardexEntity::validateNonNegativeStock);

        kardexList = this.kardexRepository.saveAll(kardexList);
        for (var kardex : kardexList) {
            this.saveInfoProduct(kardex);
        }
        return kardexList;
    }

    public KardexEntity findLastMovement(String ProductCod, String Variant, String WarehouseCod, String StoreCod) {
        return this.kardexRepository.findLastMovement(ProductCod, Variant, WarehouseCod, StoreCod);
    }

    private void saveInfoProduct(KardexEntity kardex) {
        ProductInfoEntity productInfo = this.productInfoShared.findById(
                new ProductInfoId(kardex.ProductCod, kardex.Variant, kardex.StoreCod));
        productInfo.addStock(kardex.NumStockMoved * getSign(kardex.TypeOperation));
        productInfo.addSession(getUserCod(), false);

        ProductInfoWarehouseEntity productInfoWarehouse = this.productInfoWarehouseShared.findById(
                new ProductInfoWarehouseId(kardex.ProductCod, kardex.Variant, kardex.WarehouseCod));
        productInfoWarehouse.addStock(kardex.NumStockMoved * getSign(kardex.TypeOperation));
        productInfoWarehouse.addSession(getUserCod(), false);

        this.productInfoShared.save(productInfo);
        this.productInfoWarehouseShared.save(productInfoWarehouse);
        this.productFindCreateShared.save(kardex.ProductCod, kardex.StoreCod);
    }

    private int getSign(String TypeOperation) {
        return ((TypeOperation.equals("S")) ? 1 : -1);
    }

    public ResponseWsDto regularizeAllKardex() {
        List<StoreEntity> storeList = this.storeShared.findAll();

        for (var store : storeList) {

            log.info("STORE : {}", store.StoreCod);

            List<ProductInfoEntity> productList = this.productInfoShared.findAll();

            for (var product : productList) {

                log.info("PRODUCT INFO : {} variant : {}", product.ProductCod, product.Variant);

                List<ProductInfoWarehouseEntity> ProductInfoWarehouseList = this.productInfoWarehouseShared
                        .findInfoWarehouse(
                                store.StoreCod, product.ProductCod);

                boolean existDiff = this.regularizeProductInfoWarehouse(ProductInfoWarehouseList, store);

                if (existDiff) {

                    log.info("PRODUCT : {} | VARIANT : {} | STORE : {}", product.ProductCod, product.Variant,
                            product.StoreCod);

                    ProductInfoWarehouseList = this.productInfoWarehouseShared.findInfoWarehouse(
                            store.StoreCod, product.ProductCod);

                    ProductInfoEntity productInfo = this.productInfoShared.findById(
                            new ProductInfoId(product.ProductCod, product.Variant, product.StoreCod));
                    productInfo.NumDigitalStock = ProductInfoWarehouseList.stream().mapToInt(e -> e.NumDigitalStock)
                            .sum();
                    productInfo.NumPhysicalStock = ProductInfoWarehouseList.stream().mapToInt(e -> e.NumPhysicalStock)
                            .sum();
                    this.productInfoShared.save(productInfo);
                    this.productFindCreateShared.save(product.ProductCod, product.StoreCod);
                }
            }
        }
        return new ResponseWsDto("Ok");
    }

    private boolean regularizeProductInfoWarehouse(List<ProductInfoWarehouseEntity> productInfoWarehouseList,
            StoreEntity store) {

        boolean existDiff = false;
        for (var productInfoWarehouse : productInfoWarehouseList) {

            log.info("PRODUCT : {} | VARIANT : {} | WAREHOUSE : {}", productInfoWarehouse.ProductCod,
                    productInfoWarehouse.Variant, productInfoWarehouse.WarehouseCod);

            KardexEntity kardex = this.kardexRepository.findLastMovement(
                    productInfoWarehouse.ProductCod, productInfoWarehouse.Variant, productInfoWarehouse.WarehouseCod, store.StoreCod);
            log.info("STOCK IN kardex : {}", kardex.NumStockAfter);
            log.info("STOCK IN productInfoWarehouse : {}", productInfoWarehouse.NumDigitalStock);

            if (kardex.NumStockAfter != productInfoWarehouse.NumDigitalStock) {
                productInfoWarehouse.NumDigitalStock = kardex.NumStockAfter;
                productInfoWarehouse.NumPhysicalStock = kardex.NumStockAfter;
                this.productInfoWarehouseShared.save(productInfoWarehouse);
                existDiff = true;
                log.info("UPDATE OK");
            }
        }
        log.info("EXIST DIFF : {}", existDiff);
        return existDiff;
    }

    public ResponsePageSearch findAll(String Query, int Page, String StoreCod) {

        this.searchService = new SearchService(this.kardexRepository);
        SearchDto search = new SearchDto(Query, Page, StoreCod);
        ResponsePageSearch responsePage = this.searchService.findAllStore(search, 10);

        List<KardexEntity> kardexList = (List<KardexEntity>) responsePage.resultSearch;

        List<String> ProductCodList = kardexList.stream()
                .map(KardexEntity::getProductCod)
                .distinct()
                .toList();

        List<ProductEntity> productList = this.productShared.findAllById(ProductCodList);

        List<String> SaleCodList = kardexList.stream()
                .filter(e -> e.SourceTable.equals("sale_head"))
                .map(KardexEntity::getOperationCod)
                .distinct()
                .toList();
        List<SaleHeadEntity> saleHeadList = this.saleHeadShared.findAllById(SaleCodList);

        List<String> PucharseCodList = kardexList.stream()
                .filter(e -> e.SourceTable.equals("pucharse_head"))
                .map(KardexEntity::getOperationCod)
                .distinct()
                .toList();
        List<PucharseHeadEntity> PucharseHeadList = this.pucharseHeadShared.findAllById(PucharseCodList);

        List<BusinessConfigEntityID> TypeOperationCodList = kardexList.stream()
                .map(KardexEntity::getTypeOperationCod)
                .distinct()
                .map(configCorr -> new BusinessConfigEntityID("OperationKardex", configCorr))
                .toList();

        List<BusinessConfigEntity> TypeOperationList = this.businessConfigSearchService.findAllById(TypeOperationCodList);

        List<KardexDto> kardexDtoList = new ArrayList<>();

        for (KardexEntity kardex : kardexList) {

            KardexDto kardexDto = new KardexDto();
            kardexDto.kardex = kardex;
            kardexDto.product = productList.stream()
                    .filter(e -> e.ProductCod.equals(kardex.ProductCod))
                    .findFirst()
                    .get();
            kardexDto.dataTypeOperation = TypeOperationList.stream()
                    .filter(e -> e.ConfigCorr == kardex.TypeOperationCod)
                    .findFirst()
                    .get();

            if (kardex.SourceTable.equals("pucharse_head")) {

                kardexDto.dataTransaction = PucharseHeadList.stream()
                        .filter(e -> e.PucharseCod.equals(kardex.OperationCod))
                        .findFirst()
                        .get();
            }
            if (kardex.SourceTable.equals("sale_head")) {
                kardexDto.dataTransaction = saleHeadList.stream()
                        .filter(e -> e.SaleCod.equals(kardex.OperationCod))
                        .findFirst()
                        .get();
            }
            kardexDtoList.add(kardexDto);
        }

        responsePage.resultSearch = kardexDtoList;

        return responsePage;
    }

}
