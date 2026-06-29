package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.ProductBuildException;
import com.ccadmin.app.product.model.dto.ProductConfigStoreUpdateDto;
import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.product.model.dto.ProductRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.*;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.model.entity.id.ProductPictureID;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.shared.StoreShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductCreateService extends SessionService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Autowired
    private ProductPictureRepository productPictureRepository;

    @Autowired
    private ProductBarcodeRepository productBarcodeRepository;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;
    @Autowired
    private StoreShared storeShared;

    @Autowired
    private ProductFindCreateService productFindCreateService;
    @Autowired
    private GenericQueuedService genericQueuedService;

    @Transactional
    public ProductRegisterDto save(ProductRegisterDto productRegister) {
        productRegister.product.session(getUserCod());
        productRegister.config.session(getUserCod()).ProductCod = productRegister.product.ProductCod;
        this.productOperationConfigShared.normalize(productRegister.config);

        if (!productRegister.productBarcode.ProductCod.isEmpty() && !productRegister.productBarcode.BarCode.isEmpty()) {
            Optional<ProductBarcodeEntity> productBarcode = this.productBarcodeRepository
                    .findById(productRegister.productBarcode.BarCode);
            if (productBarcode.isPresent()) {
                if (!productBarcode.get().ProductCod.equals(productRegister.product.ProductCod)) {
                    throw new ProductBuildException("Codigo de barras esta registrado con otros productos.");
                }
            }
            productRegister.productBarcode.addSession(getUserCod());
        }

        ProductVariantEntity variant = new ProductVariantEntity()
                .buildNew(productRegister.product.ProductCod)
                .session(getUserCod());
        boolean existProduct = this.productRepository.existsById(productRegister.product.ProductCod);

        this.productRepository.save(productRegister.product);
        if (existProduct) {
            productRegister.config.StoreCod = getStoreCod();
            this.productConfigRepository.save(productRegister.config);
        } else {
            this.productConfigRepository.saveAll(this.buildConfigForAllStores(productRegister.config));
        }

        if (!existProduct) {
            this.productVariantRepository.save(variant);
            this.productInfoRepository.saveAllInfo(productRegister.product.ProductCod);
            this.productInfoWarehouseRepository.saveAllInfo(productRegister.product.ProductCod);
        }
        if (!productRegister.productBarcode.ProductCod.isEmpty()) {
            this.productBarcodeRepository.save(productRegister.productBarcode);
        }

        if (productRegister.pictureList != null && productRegister.pictureList.size() > 0) {
            productRegister.pictureList.forEach(
                    e -> e.session(getUserCod()));
            this.productPictureRepository.updateAllStatus(productRegister.product.ProductCod, "I");
            this.productPictureRepository.saveAll(productRegister.pictureList);
        }
        this.productFindCreateService.generateSearch(productRegister.product.ProductCod);
        return productRegister;
    }

    @Transactional
    public ResponseWsDto saveAll(ProductRegisterMassiveDto productRegisterMassive) {
        ResponseWsDto rpt = new ResponseWsDto();
        ProductRegisterMassiveDto registerMassiveFail = new ProductRegisterMassiveDto();
        ProductRegisterMassiveDto registerMassiveExists = new ProductRegisterMassiveDto();
        ProductRegisterMassiveDto registerMassiveOk = new ProductRegisterMassiveDto();

        for (var productRegister : productRegisterMassive.productList) {
            try {
                productRegister.product.session(getUserCod()).validate();

                if (this.productRepository.existsById(productRegister.product.ProductCod)) {
                    registerMassiveExists.productList.add(productRegister);
                } else {
                    registerMassiveOk.productList.add(productRegister);
                }
            } catch (Exception ex) {
                log.error("Error en saveAll :{} ==> {}", productRegister.product.toString(), ex.getMessage());
                registerMassiveFail.productList.add(productRegister);
            }
        }

        List<String> productCodList = registerMassiveOk.productList
                .stream()
                .map(productRegister -> productRegister.product.ProductCod)
                .toList();

        List<ProductEntity> productList = registerMassiveOk.productList
                .stream()
                .map(productRegister -> productRegister.product)
                .toList();

        List<ProductConfigEntity> configList = registerMassiveOk.productList
                .stream()
                .flatMap(productRegister -> {
                    productRegister.config.session(getUserCod());
                    productRegister.config.ProductCod = productRegister.product.ProductCod;
                    this.productOperationConfigShared.normalize(productRegister.config);
                    return this.buildConfigForAllStores(productRegister.config).stream();
                })
                .toList();

        List<ProductVariantEntity> variantList = registerMassiveOk.productList
                .stream()
                .map(productRegister -> new ProductVariantEntity()
                        .buildNew(productRegister.product.ProductCod)
                        .session(getUserCod()))
                .toList();

        this.productRepository.saveAll(productList);
        this.productConfigRepository.saveAll(configList);
        this.productVariantRepository.saveAll(variantList);
        this.productInfoRepository.saveAllInfo(productCodList);
        this.productInfoWarehouseRepository.saveAllInfo(productCodList);

        generateSearchQueued(productCodList);

        rpt.AddResponseAdditional("registerMassiveFail", registerMassiveFail);
        rpt.AddResponseAdditional("registerMassiveExists", registerMassiveExists);
        return rpt;
    }

    @Transactional
    public ProductPictureEntity deletePicture(ProductPictureEntity productPicture) {

        Optional<ProductPictureEntity> productPictureServer = this.productPictureRepository.findById(
                new ProductPictureID(productPicture.ProductCod, productPicture.FileCod));

        if (productPictureServer.isPresent()) {
            productPictureServer.get().inactive(getUserCod());
            return this.productPictureRepository.save(productPictureServer.get());
        }
        return null;
    }

    public void generateSearchQueued(List<String> productCodList) {
        ProductCreateTaskService productCreateTaskService = new ProductCreateTaskService(
                this.productFindCreateService, productCodList);
        this.genericQueuedService.addQueued(productCreateTaskService);
    }

    @Transactional
    public ProductConfigStoreUpdateDto saveConfigByStores(ProductConfigStoreUpdateDto request) {
        if (request == null || request.ProductCod == null || request.ProductCod.isEmpty()) {
            throw new ProductBuildException("Debe seleccionar un producto.");
        }
        if (request.config == null) {
            throw new ProductBuildException("Debe ingresar la configuracion del producto.");
        }

        ProductConfigEntity baseConfig = this.productConfigRepository.findAnyByProductCod(request.ProductCod);
        List<String> storeCodList = resolveTargetStores(request);

        for (String storeCod : storeCodList) {
            ProductConfigEntity config = this.productConfigRepository
                    .findById(new ProductConfigID(request.ProductCod, storeCod))
                    .orElseGet(() -> this.buildConfigForStore(
                            baseConfig != null ? baseConfig : request.config,
                            storeCod
                    ));

            config.ProductCod = request.ProductCod;
            config.StoreCod = storeCod;
            config.NumPrice = request.config.NumPrice;
            config.NumMaxStock = request.config.NumMaxStock;
            config.NumMinStock = request.config.NumMinStock;
            config.ProductUnitName = request.config.ProductUnitName;
            config.ProductUnitFactor = request.config.ProductUnitFactor;
            config.session(getUserCod());
            this.productOperationConfigShared.normalize(config);
            this.productConfigRepository.save(config);
            this.productFindCreateService.save(request.ProductCod, storeCod);
        }

        return request;
    }

    private List<String> resolveTargetStores(ProductConfigStoreUpdateDto request) {
        if (request.ApplyAllStores) {
            return this.storeShared.findAll().stream().map(store -> store.StoreCod).toList();
        }
        if (request.StoreCod != null && !request.StoreCod.isEmpty()) {
            return List.of(request.StoreCod);
        }
        if (request.StoreCodList != null && !request.StoreCodList.isEmpty()) {
            return request.StoreCodList;
        }
        throw new ProductBuildException("Debe seleccionar al menos una tienda.");
    }

    private List<ProductConfigEntity> buildConfigForAllStores(ProductConfigEntity source) {
        return this.storeShared.findAll()
                .stream()
                .map(store -> this.buildConfigForStore(source, store.StoreCod))
                .toList();
    }

    private ProductConfigEntity buildConfigForStore(ProductConfigEntity source, String storeCod) {
        ProductConfigEntity config = new ProductConfigEntity();
        config.ProductCod = source.ProductCod;
        config.StoreCod = storeCod;
        config.NumPrice = source.NumPrice;
        config.NumMaxStock = source.NumMaxStock;
        config.NumMinStock = source.NumMinStock;
        config.IsDiscontable = source.IsDiscontable;
        config.DiscountType = source.DiscountType;
        config.NumDiscountMax = source.NumDiscountMax;
        config.ProductUnitName = source.ProductUnitName;
        config.ProductUnitFactor = source.ProductUnitFactor;
        config.Version = source.Version;
        config.session(getUserCod());
        this.productOperationConfigShared.normalize(config);
        return config;
    }
}
