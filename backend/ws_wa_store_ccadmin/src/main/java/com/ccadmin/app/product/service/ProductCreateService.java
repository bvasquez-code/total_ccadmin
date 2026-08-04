package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.ProductBuildException;
import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.product.model.dto.ProductRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.*;
import com.ccadmin.app.product.model.entity.id.ProductPictureID;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.TableSequenceShared;
import com.ccadmin.app.system.utility.StringUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@Slf4j
public class ProductCreateService extends SessionService {

    private static final String PRODUCT_SEQUENCE_TYPE = "product";

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
    @Autowired
    private ProductTaxConfigCreateService productTaxConfigCreateService;
    @Autowired
    private TableSequenceShared tableSequenceShared;

    @Transactional
    public ProductRegisterDto save(ProductRegisterDto productRegister) {
        ensureProductCode(productRegister);
        productRegister.product.session(getUserCod());
        productRegister.config.session(getUserCod()).ProductCod = productRegister.product.ProductCod;
        try {
            this.productOperationConfigShared.validateDigitalIndicator(productRegister.config);
        } catch (IllegalArgumentException exception) {
            throw new ProductBuildException(exception.getMessage());
        }

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
        if (existProduct && !productRegister.IsEditMode) {
            throw new ProductBuildException("Codigo de producto ya existe.");
        }
        if (existProduct) {
            productRegister.config.StoreCod = getStoreCod();
            try {
                this.productOperationConfigShared.validateDigitalConversion(
                        productRegister.config.ProductCod,
                        productRegister.config.StoreCod,
                        productRegister.config.IsDigital
                );
            } catch (IllegalArgumentException exception) {
                throw new ProductBuildException(exception.getMessage());
            }
        }

        this.productRepository.save(productRegister.product);
        if (existProduct) {
            this.productConfigRepository.save(productRegister.config);
            this.productTaxConfigCreateService.ensureDefaultMainTax(productRegister.config.ProductCod, productRegister.config.StoreCod);
        } else {
            List<ProductConfigEntity> configList = this.buildConfigForAllStores(productRegister.config);
            this.productConfigRepository.saveAll(configList);
            configList.forEach(config -> this.productTaxConfigCreateService.ensureDefaultMainTax(config.ProductCod, config.StoreCod));
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

    public String generateProductCode() {
        return this.tableSequenceShared.getNextAvailableCode(
                PRODUCT_SEQUENCE_TYPE, this.productRepository::existsById
        );
    }

    private void ensureProductCode(ProductRegisterDto productRegister) {
        if (productRegister == null || productRegister.product == null) {
            throw new ProductBuildException("Debe ingresar un producto.");
        }
        if (StringUtil.isBlank(productRegister.product.ProductCod)) {
            productRegister.product.ProductCod = this.generateProductCode();
            if (productRegister.config != null) {
                productRegister.config.ProductCod = productRegister.product.ProductCod;
            }
            if (productRegister.productBarcode != null) {
                productRegister.productBarcode.ProductCod = productRegister.product.ProductCod;
            }
        }
    }

    @Transactional
    public ResponseWsDto saveAll(ProductRegisterMassiveDto productRegisterMassive) {
        ResponseWsDto rpt = new ResponseWsDto();
        ProductRegisterMassiveDto registerMassiveFail = new ProductRegisterMassiveDto();
        ProductRegisterMassiveDto registerMassiveExists = new ProductRegisterMassiveDto();
        ProductRegisterMassiveDto registerMassiveOk = new ProductRegisterMassiveDto();

        for (var productRegister : productRegisterMassive.productList) {
            try {
                productRegister.product.validate();

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

        if (!registerMassiveOk.productList.isEmpty()) {
            createBulk(registerMassiveOk, getUserCod(), true);
        }

        rpt.AddResponseAdditional("registerMassiveFail", registerMassiveFail);
        rpt.AddResponseAdditional("registerMassiveExists", registerMassiveExists);
        return rpt;
    }

    /**
     * Crea un bloque ya validado y genera product_search dentro del mismo
     * procesamiento en segundo plano de BulkLoad.
     */
    @Transactional
    public List<String> createBulk(ProductRegisterMassiveDto productRegisterMassive,
                                   String userCod) {
        return createBulk(productRegisterMassive, userCod, false);
    }

    private List<String> createBulk(
            ProductRegisterMassiveDto productRegisterMassive,
            String userCod,
            boolean queueProductSearch
    ) {
        if (productRegisterMassive == null
                || productRegisterMassive.productList == null
                || productRegisterMassive.productList.isEmpty()) {
            throw new IllegalArgumentException(
                    "El bloque de productos no tiene detalles"
            );
        }
        String auditUser = StringUtil.isBlank(userCod)
                ? "SISTEMA" : userCod.trim();
        List<ProductEntity> productList = new ArrayList<>();
        List<ProductConfigEntity> configList = new ArrayList<>();
        List<ProductVariantEntity> variantList = new ArrayList<>();
        List<ProductBarcodeEntity> barcodeList = new ArrayList<>();
        List<String> productCodList = new ArrayList<>();

        for (ProductRegisterDto productRegister : productRegisterMassive.productList) {
            if (productRegister == null || productRegister.product == null
                    || productRegister.config == null) {
                throw new IllegalArgumentException(
                        "El detalle de producto esta incompleto"
                );
            }
            productRegister.product.session(auditUser).validate();
            String productCod = productRegister.product.ProductCod;
            if (productRepository.existsById(productCod)) {
                throw new ProductBuildException(
                        "Codigo de producto ya existe: " + productCod
                );
            }

            productRegister.config.ProductCod = productCod;
            productRegister.config.session(auditUser);
            productOperationConfigShared.validateDigitalIndicator(productRegister.config);
            productList.add(productRegister.product);
            configList.addAll(buildConfigForAllStores(
                    productRegister.config, auditUser
            ));
            variantList.add(new ProductVariantEntity()
                    .buildNew(productCod)
                    .session(auditUser));
            productCodList.add(productCod);

            ProductBarcodeEntity barcode = productRegister.productBarcode;
            if (barcode != null && !StringUtil.isBlank(barcode.BarCode)) {
                Optional<ProductBarcodeEntity> existingBarcode =
                        productBarcodeRepository.findById(barcode.BarCode);
                if (existingBarcode.isPresent()) {
                    throw new ProductBuildException(
                            "Codigo de barras ya registrado: " + barcode.BarCode
                    );
                }
                barcode.ProductCod = productCod;
                barcode.addSessionCreate(auditUser);
                barcodeList.add(barcode);
            }
        }

        productRepository.saveAll(productList);
        productConfigRepository.saveAll(configList);
        configList.forEach(config ->
                productTaxConfigCreateService.ensureDefaultMainTax(
                        config.ProductCod, config.StoreCod
                )
        );
        productVariantRepository.saveAll(variantList);
        productInfoRepository.saveAllInfo(productCodList);
        productInfoWarehouseRepository.saveAllInfo(productCodList);
        if (!barcodeList.isEmpty()) {
            productBarcodeRepository.saveAll(barcodeList);
        }

        if (queueProductSearch) {
            generateSearchQueued(productCodList);
        } else {
            productCodList.forEach(productCod ->
                    productFindCreateService.generateSearch(productCod, auditUser)
            );
        }
        return productCodList;
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

    private List<ProductConfigEntity> buildConfigForAllStores(ProductConfigEntity source) {
        return buildConfigForAllStores(source, getUserCod());
    }

    private List<ProductConfigEntity> buildConfigForAllStores(
            ProductConfigEntity source,
            String userCod
    ) {
        return this.storeShared.findAll()
                .stream()
                .map(store -> this.buildConfigForStore(
                        source, store.StoreCod, userCod
                ))
                .toList();
    }

    private ProductConfigEntity buildConfigForStore(ProductConfigEntity source, String storeCod) {
        return buildConfigForStore(source, storeCod, getUserCod());
    }

    private ProductConfigEntity buildConfigForStore(
            ProductConfigEntity source,
            String storeCod,
            String userCod
    ) {
        ProductConfigEntity config = new ProductConfigEntity();
        config.ProductCod = source.ProductCod;
        config.StoreCod = storeCod;
        config.NumPrice = source.NumPrice;
        config.NumMaxStock = source.NumMaxStock;
        config.NumMinStock = source.NumMinStock;
        config.IsDigital = source.IsDigital;
        config.IsDiscontable = source.IsDiscontable;
        config.DiscountType = source.DiscountType;
        config.NumDiscountMax = source.NumDiscountMax;
        config.ProductUnitName = source.ProductUnitName;
        config.ProductUnitFactor = source.ProductUnitFactor;
        config.Version = source.Version;
        config.session(userCod);
        this.productOperationConfigShared.normalize(config);
        return config;
    }
}
