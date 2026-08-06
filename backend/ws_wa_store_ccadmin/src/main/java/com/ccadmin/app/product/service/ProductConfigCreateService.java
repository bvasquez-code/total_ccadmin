package com.ccadmin.app.product.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.product.exception.ProductBuildException;
import com.ccadmin.app.product.model.dto.*;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductRepository;
import com.ccadmin.app.product.shared.ProductCategoryDigitalPolicy;
import com.ccadmin.app.product.shared.ProductDiscountPolicy;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.store.shared.StoreShared;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ProductConfigCreateService extends SessionService {
    private final ProductConfigRepository productConfigRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ProductOperationConfigShared productOperationConfigShared;
    private final StoreShared storeShared;
    private final ProductFindCreateService productFindCreateService;
    private final ProductTaxConfigCreateService productTaxConfigCreateService;
    private final ProductDiscountPolicy productDiscountPolicy;
    private final ProductCategoryDigitalPolicy productCategoryDigitalPolicy;

    public ProductConfigCreateService(ProductConfigRepository productConfigRepository,
                                      ProductRepository productRepository,
                                      StoreRepository storeRepository,
                                      ProductOperationConfigShared productOperationConfigShared,
                                      StoreShared storeShared,
                                      ProductFindCreateService productFindCreateService,
                                      ProductTaxConfigCreateService productTaxConfigCreateService,
                                      ProductDiscountPolicy productDiscountPolicy,
                                      ProductCategoryDigitalPolicy productCategoryDigitalPolicy) {
        this.productConfigRepository = productConfigRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.productOperationConfigShared = productOperationConfigShared;
        this.storeShared = storeShared;
        this.productFindCreateService = productFindCreateService;
        this.productTaxConfigCreateService = productTaxConfigCreateService;
        this.productDiscountPolicy = productDiscountPolicy;
        this.productCategoryDigitalPolicy = productCategoryDigitalPolicy;
    }

    @Transactional
    public ProductConfigStoreUpdateDto saveConfigByStores(ProductConfigStoreUpdateDto request) {
        if (request == null || clean(request.ProductCod).isEmpty()) {
            throw new ProductBuildException("Debe seleccionar un producto.");
        }
        if (request.config == null) {
            throw new ProductBuildException("Debe ingresar la configuracion del producto.");
        }
        try {
            ProductEntity product = this.productRepository.findByProductCodNative(request.ProductCod)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe el producto " + request.ProductCod
                    ));
            this.productCategoryDigitalPolicy.apply(product.CategoryCod, request.config);
            this.productDiscountPolicy.normalizeAndValidate(request.config);
            this.productOperationConfigShared.validateDigitalIndicator(request.config);
        } catch (IllegalArgumentException exception) {
            throw new ProductBuildException(exception.getMessage());
        }

        ProductConfigEntity baseConfig =
                productConfigRepository.findAnyByProductCod(request.ProductCod);
        List<String> storeCodList = resolveTargetStores(request);
        String userCod = getUserCod();

        for (String storeCod : storeCodList) {
            ProductConfigEntity config = productConfigRepository.findForUpdate(request.ProductCod, storeCod);
            try {
                productOperationConfigShared.validateDigitalConversion(
                        config, request.ProductCod, storeCod, request.config.IsDigital
                );
            } catch (IllegalArgumentException exception) {
                throw new ProductBuildException(exception.getMessage());
            }
            if (config == null) {
                config = buildConfigForStore(
                        baseConfig != null ? baseConfig : request.config,
                        storeCod,
                        userCod
                );
            }
            copyEditableConfig(request.config, config);
            config.ProductCod = request.ProductCod;
            config.StoreCod = storeCod;
            config.session(userCod);
            productOperationConfigShared.normalize(config);
            productConfigRepository.save(config);
            saveTaxes(request, config);
            productFindCreateService.save(request.ProductCod, storeCod, userCod);
        }
        return request;
    }

    /**
     * Valida y convierte una carga de precios a la mascara generica de BulkLoad.
     * No persiste la carga ni modifica precios.
     */
    public BulkLoadPreparedDto prepareBulkPriceLoad(BulkLoadParsedRequestDto request) {
        PricePreparationContext context = new PricePreparationContext();
        Map<String, StoreEntity> activeStoreMap = new LinkedHashMap<>();
        storeRepository.findAllActive().forEach(store -> activeStoreMap.put(store.StoreCod, store));
        resolvePriceStores(request, activeStoreMap, context);
        validatePriceRows(request, context);
        validatePriceConfigurations(request, context);

        BulkLoadPreparedDto prepared = new BulkLoadPreparedDto();
        prepared.StoreCodList.addAll(context.storeList.stream().map(item -> item.StoreCod).toList());
        prepared.ErrorList.addAll(context.errorList);
        List<String> stores = prepared.StoreCodList.isEmpty()
                ? Collections.singletonList(null) : prepared.StoreCodList;

        for (BulkLoadSourceRowDto row : request.RowList) {
            for (String storeCod : stores) {
                BulkLoadPreparedDetailDto detail = new BulkLoadPreparedDetailDto();
                detail.SourceRowNumber = sourceRow(row.RowNumber);
                detail.StoreCod = storeCod;
                detail.BusinessKey = trimToLength(
                        clean(row.ProductCod) + (storeCod == null ? "" : "|" + storeCod), 128
                );
                detail.Payload.put("ProductCod", clean(row.ProductCod));
                detail.Payload.put("StoreCod", storeCod);
                detail.Payload.put("NumPrice",
                        context.priceMap.getOrDefault(row.RowNumber, row.Value));
                detail.ErrorList.addAll(context.errorsFor(row.RowNumber, storeCod));
                prepared.DetailList.add(detail);
            }
        }
        return prepared;
    }

    /**
     * Aplica un bloque ya validado de precios. El trigger de product_config
     * conserva el historico y luego se regenera product_search.
     */
    @Transactional
    public List<ProductConfigBulkPriceResultDto> saveBulkPrices(
            ProductConfigBulkPriceUpdateDto request,
            String userCod
    ) {
        if (request == null || request.DetailList == null || request.DetailList.isEmpty()) {
            throw new IllegalArgumentException("El bloque de precios no tiene detalles");
        }
        String auditUser = clean(userCod).isEmpty() ? "SISTEMA" : userCod.trim();
        Set<String> productSet = new LinkedHashSet<>();
        List<ProductConfigBulkPriceResultDto> resultList = new ArrayList<>();

        for (ProductConfigBulkPriceLineDto line : request.DetailList) {
            ProductConfigEntity config = productConfigRepository.findForUpdate(
                    clean(line.ProductCod), clean(line.StoreCod)
            );
            if (config == null) {
                throw new IllegalStateException(
                        "No existe la configuracion de " + line.ProductCod
                                + " en " + line.StoreCod
                );
            }
            BigDecimal oldPrice = config.NumPrice;
            config.NumPrice = line.NumPrice.setScale(2);
            config.addSessionModify(auditUser);
            productConfigRepository.save(config);

            ProductConfigBulkPriceResultDto result = new ProductConfigBulkPriceResultDto();
            result.ReferenceItemNumber = line.ReferenceItemNumber;
            result.OldPrice = oldPrice;
            result.NewPrice = config.NumPrice;
            result.Changed = oldPrice == null || oldPrice.compareTo(config.NumPrice) != 0;
            resultList.add(result);
            productSet.add(config.ProductCod);
        }
        productConfigRepository.flush();
        productSet.forEach(product -> productFindCreateService.generateSearch(product, auditUser));
        return resultList;
    }

    private void resolvePriceStores(BulkLoadParsedRequestDto request,
                                    Map<String, StoreEntity> activeStoreMap,
                                    PricePreparationContext context) {
        if (request.StoreList.isEmpty()) {
            context.globalError(error("LOCALES", 0, null, "StoreCod", "",
                    "STORE_REQUIRED", "Debe indicar al menos un local"));
            return;
        }
        List<String> normalizedList = request.StoreList.stream()
                .map(item -> clean(item.StoreCod).toUpperCase(Locale.ROOT))
                .toList();
        boolean hasAll = normalizedList.contains(BulkLoadConstants.WILDCARD_ALL);
        if (hasAll) {
            if (request.StoreList.size() != 1) {
                context.globalError(error("LOCALES",
                        rowForStore(request.StoreList, BulkLoadConstants.WILDCARD_ALL),
                        null, "StoreCod", BulkLoadConstants.WILDCARD_ALL,
                        "STORE_WILDCARD_EXCLUSIVE",
                        "TODOS debe ser el unico registro de la hoja LOCALES"));
                return;
            }
            context.storeList.addAll(activeStoreMap.values());
            if (context.storeList.isEmpty()) {
                context.globalError(error("LOCALES", sourceRow(request.StoreList.getFirst().RowNumber),
                        null, "StoreCod", BulkLoadConstants.WILDCARD_ALL,
                        "STORE_NOT_FOUND", "No existen locales activos"));
            }
            return;
        }

        Set<String> seen = new HashSet<>();
        for (BulkLoadStoreRowDto item : request.StoreList) {
            String storeCod = clean(item.StoreCod).toUpperCase(Locale.ROOT);
            if (storeCod.isBlank()) {
                context.globalError(error("LOCALES", sourceRow(item.RowNumber), null,
                        "StoreCod", "", "STORE_REQUIRED",
                        "El codigo de local es obligatorio"));
            } else if (storeCod.length() > 4) {
                context.globalError(error("LOCALES", sourceRow(item.RowNumber), storeCod,
                        "StoreCod", storeCod, "STORE_LENGTH",
                        "El codigo de local admite hasta 4 caracteres"));
            } else if (!seen.add(storeCod)) {
                context.globalError(error("LOCALES", sourceRow(item.RowNumber), storeCod,
                        "StoreCod", storeCod, "STORE_DUPLICATED",
                        "El local esta repetido"));
            } else {
                StoreEntity store = activeStoreMap.get(storeCod);
                if (store == null) {
                    context.globalError(error("LOCALES", sourceRow(item.RowNumber), storeCod,
                            "StoreCod", storeCod, "STORE_NOT_FOUND",
                            "El local no existe o esta inactivo"));
                } else {
                    context.storeList.add(store);
                }
            }
        }
    }

    private void validatePriceRows(BulkLoadParsedRequestDto request,
                                   PricePreparationContext context) {
        if (request.RowList.isEmpty()) {
            context.globalError(error("PRODUCTO_PRECIO", 0, null,
                    "ProductCod", "", "ROW_REQUIRED",
                    "El archivo debe contener al menos un producto"));
            return;
        }
        Set<String> seenProductSet = new HashSet<>();
        for (BulkLoadSourceRowDto row : request.RowList) {
            int rowNumber = sourceRow(row.RowNumber);
            String productCod = clean(row.ProductCod);
            if (productCod.isBlank()) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "ProductCod", "", "PRODUCT_REQUIRED",
                        "El codigo de producto es obligatorio"));
            } else if (productCod.length() > 20) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "ProductCod", productCod, "PRODUCT_LENGTH",
                        "El codigo de producto admite hasta 20 caracteres"));
            } else if (!seenProductSet.add(productCod)) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "ProductCod", productCod, "PRODUCT_DUPLICATED",
                        "El producto esta repetido en el archivo"));
            }

            ProductEntity product = productCod.isBlank()
                    ? null : productRepository.findById(productCod).orElse(null);
            if (product == null || !StatusConst.ACTIVE.equals(product.Status)) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "ProductCod", productCod, "PRODUCT_NOT_FOUND",
                        "El producto no existe o esta inactivo"));
            }

            BigDecimal price = parseDecimal(row.Value);
            if (price == null) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "NumPrice", row.Value, "NUMBER_FORMAT",
                        "El precio debe ser numerico"));
            } else if (price.compareTo(BigDecimal.ZERO) <= 0
                    || price.scale() > 2
                    || price.precision() - price.scale() > 14) {
                context.rowError(row.RowNumber, error("PRODUCTO_PRECIO", rowNumber, null,
                        "NumPrice", row.Value, "PRICE_FORMAT",
                        "El precio debe cumplir NUMERO(16,2) y ser mayor a cero"));
            } else {
                context.priceMap.put(row.RowNumber,
                        price.setScale(2, RoundingMode.UNNECESSARY));
            }
        }
    }

    private void validatePriceConfigurations(BulkLoadParsedRequestDto request,
                                             PricePreparationContext context) {
        for (BulkLoadSourceRowDto row : request.RowList) {
            String productCod = clean(row.ProductCod);
            for (StoreEntity store : context.storeList) {
                ProductConfigEntity config = productConfigRepository.findById(
                        new ProductConfigID(productCod, store.StoreCod)
                ).orElse(null);
                if (config == null || !StatusConst.ACTIVE.equals(config.Status)) {
                    context.combinationError(row.RowNumber, store.StoreCod,
                            error("PRODUCTO_PRECIO", sourceRow(row.RowNumber),
                                    store.StoreCod, "ProductCod", productCod,
                                    "PRODUCT_CONFIG_NOT_FOUND",
                                    "El producto no tiene configuracion activa en el local"));
                }
            }
        }
    }

    private void saveTaxes(ProductConfigStoreUpdateDto request, ProductConfigEntity config) {
        if (request.TaxConfigList != null && !request.TaxConfigList.isEmpty()) {
            ProductTaxConfigRegisterDto taxRequest = new ProductTaxConfigRegisterDto();
            taxRequest.ProductCod = config.ProductCod;
            taxRequest.StoreCod = config.StoreCod;
            taxRequest.TaxConfigList = copyTaxConfigList(request.TaxConfigList);
            productTaxConfigCreateService.saveAllByProductStore(taxRequest);
        } else {
            productTaxConfigCreateService.ensureDefaultMainTax(
                    config.ProductCod, config.StoreCod
            );
        }
    }

    private List<ProductTaxConfigEntity> copyTaxConfigList(
            List<ProductTaxConfigEntity> sourceList
    ) {
        return sourceList.stream().map(source -> {
            ProductTaxConfigEntity copy = new ProductTaxConfigEntity();
            copy.ProductCod = source.ProductCod;
            copy.StoreCod = source.StoreCod;
            copy.TaxCod = source.TaxCod;
            copy.TaxAffectationCod = source.TaxAffectationCod;
            copy.IsMainTax = source.IsMainTax;
            copy.TaxRateValue = source.TaxRateValue;
            copy.FixedUnitAmount = source.FixedUnitAmount;
            copy.TaxCalculationType = source.TaxCalculationType;
            copy.IsInformative = source.IsInformative;
            copy.CalculationOrder = source.CalculationOrder;
            copy.Status = source.Status;
            return copy;
        }).toList();
    }

    private List<String> resolveTargetStores(ProductConfigStoreUpdateDto request) {
        if (request.ApplyAllStores) {
            return storeShared.findAll().stream().map(store -> store.StoreCod).toList();
        }
        if (!clean(request.StoreCod).isEmpty()) return List.of(request.StoreCod);
        if (request.StoreCodList != null && !request.StoreCodList.isEmpty()) {
            return request.StoreCodList;
        }
        throw new ProductBuildException("Debe seleccionar al menos una tienda.");
    }

    private ProductConfigEntity buildConfigForStore(ProductConfigEntity source,
                                                     String storeCod,
                                                     String userCod) {
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
        productOperationConfigShared.normalize(config);
        return config;
    }

    private void copyEditableConfig(ProductConfigEntity source,
                                    ProductConfigEntity target) {
        target.NumPrice = source.NumPrice;
        target.NumMaxStock = source.NumMaxStock;
        target.NumMinStock = source.NumMinStock;
        target.IsDigital = source.IsDigital;
        target.ProductUnitName = source.ProductUnitName;
        target.ProductUnitFactor = source.ProductUnitFactor;
        target.IsDiscontable = source.IsDiscontable;
        target.DiscountType = source.DiscountType;
        target.NumDiscountMax = source.NumDiscountMax;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            String normalized = clean(value).replace(",", ".");
            if (normalized.isBlank()) return null;
            return new BigDecimal(normalized).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BulkLoadErrorDto error(String sheet, Integer row, String store, String field,
                                   String value, String code, String detail) {
        return new BulkLoadErrorDto(sheet, row, store, field, value, code, detail);
    }

    private int rowForStore(List<BulkLoadStoreRowDto> list, String storeCod) {
        return list.stream()
                .filter(item -> storeCod.equalsIgnoreCase(clean(item.StoreCod)))
                .map(item -> sourceRow(item.RowNumber))
                .findFirst().orElse(0);
    }

    private int sourceRow(Integer rowNumber) {
        return rowNumber == null || rowNumber < 1 ? 1 : rowNumber;
    }

    private String trimToLength(String value, int length) {
        String cleanValue = clean(value);
        return cleanValue.length() <= length
                ? cleanValue : cleanValue.substring(0, length);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static class PricePreparationContext {
        private final List<StoreEntity> storeList = new ArrayList<>();
        private final List<BulkLoadErrorDto> errorList = new ArrayList<>();
        private final List<BulkLoadErrorDto> globalErrors = new ArrayList<>();
        private final Map<Integer, List<BulkLoadErrorDto>> rowErrorMap = new HashMap<>();
        private final Map<String, List<BulkLoadErrorDto>> combinationErrorMap = new HashMap<>();
        private final Map<Integer, Object> priceMap = new HashMap<>();

        private void globalError(BulkLoadErrorDto error) {
            errorList.add(error);
            globalErrors.add(error);
        }

        private void rowError(Integer row, BulkLoadErrorDto error) {
            errorList.add(error);
            rowErrorMap.computeIfAbsent(keyRow(row), ignored -> new ArrayList<>()).add(error);
        }

        private void combinationError(Integer row, String store, BulkLoadErrorDto error) {
            errorList.add(error);
            combinationErrorMap.computeIfAbsent(key(row, store), ignored -> new ArrayList<>())
                    .add(error);
        }

        private List<BulkLoadErrorDto> errorsFor(Integer row, String store) {
            List<BulkLoadErrorDto> errors = new ArrayList<>(globalErrors);
            errors.addAll(rowErrorMap.getOrDefault(keyRow(row), List.of()));
            errors.addAll(combinationErrorMap.getOrDefault(key(row, store), List.of()));
            return errors;
        }

        private static int keyRow(Integer row) {
            return row == null ? 0 : row;
        }

        private static String key(Integer row, String store) {
            return keyRow(row) + "|" + (store == null ? "" : store);
        }
    }
}
